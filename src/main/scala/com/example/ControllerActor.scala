package com.example

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.example.WorkerActor.{ControlMessage, DataMessage, WorkerMessage}
import com.example.messages.{AddElementToArray, PrintArray, PrintString, SendControlToWorker, SendDataToWorker, SendToController}

import scala.collection.mutable

object ControllerActor {

  trait ControllerMessage{
    val sender:String
    val seq:Long
  }

  case class Log(sender:String, seq:Long, payload:RunnableMessage, dataCursor:Long) extends ControllerMessage

  case class Execute(sender:String, seq:Long, payload:RunnableMessage) extends ControllerMessage

  def apply(mainFunc:MutableState => Unit): Behavior[ControllerMessage] = Behaviors.setup(context => new ControllerBehavior(mainFunc, context))

  class ControllerBehavior(mainFunc:MutableState => Unit, context: ActorContext[ControllerMessage]) extends AbstractBehavior[ControllerMessage](context){
    val worker: ActorRef[WorkerMessage] = context.spawn(WorkerActor(context.self),"worker")
    val output = new MutableState("controller", context.self, worker, context)
    val controlFIFOGate = new mutable.AnyRefMap[String,OrderingEnforcer[ControllerMessage]]()
    val logs = mutable.Queue[Log]()

    GlobalControl.controllerState = output
    GlobalControl.controllerLog = logs

    override def onMessage(msg: ControllerMessage): Behavior[ControllerMessage] = {
      OrderingEnforcer.reorderMessage(controlFIFOGate, msg.sender, msg.seq, msg).foreach{
          i =>
          i.foreach{
            m =>
              println(s"------------------- controller received message with seq = ${m.seq} from ${m.sender}-------------------")
              handle(m)
          }
      }
      Behaviors.same
    }


    def handle(controllerMessage: ControllerMessage): Unit ={
      controllerMessage match{
        case Execute(_,_,payload) =>
          payload.invoke(output)
        case msg:Log =>
          logs.enqueue(msg)
      }
    }

    //main() starts here:
    mainFunc(output)
  }

}