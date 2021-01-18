package com.example

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.example.WorkerActor.{ControlMessage, DataMessage, WorkerMessage}
import com.example.messages.{AddElementToArray, PrintArray, PrintString, SendControlToWorker, SendDataToWorker, SendToController}

import scala.collection.mutable

object ControllerActor {

  trait ControllerMessage

  case class Log(sender:String, seq:Long, id:Long, dataCursor:Long) extends FIFOMessage with ControllerMessage

  case class Execute(sender:String, seq:Long, payload:RunnableMessage[ControllerOutputChannel]) extends FIFOMessage with ControllerMessage

  case class RecoverWorker() extends ControllerMessage

  def apply(mainFunc:ControllerOutputChannel => Unit): Behavior[ControllerMessage] = Behaviors.setup(context => new ControllerBehavior(mainFunc, context))

  class ControllerBehavior(mainFunc:ControllerOutputChannel => Unit, context: ActorContext[ControllerMessage]) extends AbstractBehavior[ControllerMessage](context){
    var worker: ActorRef[WorkerMessage] = context.spawn(WorkerActor(context.self),"worker")
    val controlFIFOGate = new mutable.AnyRefMap[String,OrderingEnforcer[FIFOMessage]]()
    val outputChannel = new ControllerOutputChannel(worker)
    val state:MutableState = new MutableState()

    GlobalControl.controllerState = state
    GlobalControl.controllerOutput = outputChannel
    GlobalControl.controllerRef = context.self

    override def onMessage(msg: ControllerMessage): Behavior[ControllerMessage] = {
      msg match{
        case message: FIFOMessage =>
          handleFIFOMessage(message)
        case recoverWorker: RecoverWorker =>
          context.stop(worker)
          worker = context.spawn(WorkerActor(context.self, outputChannel.logs.clone()),"worker")
          outputChannel.resendUnLoggedControlMessages()
          outputChannel.resendDataMessages()
      }
      Behaviors.same
    }


    def handleFIFOMessage(m:FIFOMessage): Unit ={
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


    //main() starts here:
    mainFunc(outputChannel)
  }

}