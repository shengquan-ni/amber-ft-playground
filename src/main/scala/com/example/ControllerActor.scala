package com.example

import java.io.{FileInputStream, ObjectInputStream}
import java.util.concurrent.Executors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.example.WorkerActor.{ControlMessage, DataMessage, WorkerMessage}
import com.example.messages.{AddElementToArray, PrintArray, PrintString, SendControlToWorker, SendDataToWorker, SendToController}

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.mutable

object ControllerActor {

  trait ControllerMessage
  case class Log(sender:String, seq:Long, id:Long, dataCursor:Long) extends FIFOMessage with ControllerMessage
  case class Execute(sender:String, seq:Long, payload:RunnableMessage[ControllerOutputChannel]) extends FIFOMessage with ControllerMessage
  case class RecoverWorker(checkPoint:String = "") extends ControllerMessage

  def apply(mainFunc:ControllerOutputChannel => Unit): Behavior[ControllerMessage] = Behaviors.setup(context => new ControllerBehavior(mainFunc, context))

  class ControllerBehavior(mainFunc:ControllerOutputChannel => Unit, context: ActorContext[ControllerMessage]) extends AbstractBehavior[ControllerMessage](context){
    var worker: ActorRef[WorkerMessage] = context.spawn(WorkerActor("worker", context.self),"worker")
    val controlFIFOGate = new mutable.AnyRefMap[String,OrderingEnforcer[FIFOMessage]]()
    val outputChannel = new ControllerOutputChannel(worker)
    val state:MutableState = new MutableState(null,null,null)
    var ver = 0

    GlobalControl.controllerState = state
    GlobalControl.controllerOutput = outputChannel
    GlobalControl.controllerRef = context.self

    override def onMessage(msg: ControllerMessage): Behavior[ControllerMessage] = {
      msg match{
        case message: FIFOMessage =>
          handleFIFOMessage(message)
        case recoverWorker: RecoverWorker =>
          recover(recoverWorker.checkPoint)
      }
      Behaviors.same
    }


    def handleFIFOMessage(m:FIFOMessage): Unit ={
      println(m)

      OrderingEnforcer.reorderMessage(controlFIFOGate, m.sender, m.seq, m).foreach{
        i =>
          i.foreach{
            case e: Execute =>
              println(s"------------------- controller received message with seq = ${m.seq} from ${m.sender}-------------------")
              e.payload.invoke(state, outputChannel)
            case Log(sender, seq, id, dataCursor) =>
              println(s"------------------- controller received message with seq = ${m.seq} from ${m.sender}-------------------")
              outputChannel.logControl(id, dataCursor)
          }
      }
    }


    def recover(checkpointPath:String): Unit ={
      ver += 1
      context.stop(worker)
      if(checkpointPath.nonEmpty && new java.io.File(checkpointPath).exists()){
        val ois = new ObjectInputStream(new FileInputStream(checkpointPath))
        try{
          val state = ois.readObject.asInstanceOf[MutableState]
          ois.close()
          val logs = outputChannel.logs.filter(l => l._2 >= state.dataCursor).clone()
          worker = context.spawnAnonymous(WorkerActor(s"worker(recovered version = $ver with checkpoint)", context.self, logs, state))
          outputChannel.worker = worker
          outputChannel.resendDataMessages(state.dataCursor)
          outputChannel.resendUnLoggedControlMessages()
        }catch{
          case e:Throwable =>
            normalRecovery()
        }
      }else{
        normalRecovery()
      }
    }

    def normalRecovery(): Unit ={
      worker = context.spawnAnonymous(WorkerActor(s"worker(recovered version = $ver)", context.self, outputChannel.logs, null))
      outputChannel.worker = worker
      outputChannel.resendDataMessages(0)
      outputChannel.resendUnLoggedControlMessages()
    }


    //main() starts here:
    implicit val ctx = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
    Future{
      mainFunc(outputChannel)
    }
  }

}