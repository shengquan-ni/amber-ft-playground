package com.example

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.example.WorkerActor.{ControlMessage, DataMessage, WorkerMessage}
import com.example.messages.{PrintString, SendControlToWorker, SendDataToWorker, SendToController, Suspend}

object ControllerActor {

  case class ControllerMessage(seq:Long, payload:RunnableMessage)

  def apply(): Behavior[ControllerMessage] = Behaviors.setup(context => new ControllerBehavior(context))

  class ControllerBehavior(context: ActorContext[ControllerMessage]) extends AbstractBehavior[ControllerMessage](context){
    val worker: ActorRef[WorkerMessage] = context.spawn(WorkerActor(context.self),"worker")
    val output = new MutableState(context.self, worker, context)
    val controlFIFOGate = new OrderingEnforcer[RunnableMessage]

    override def onMessage(msg: ControllerMessage): Behavior[ControllerMessage] = {
      controlFIFOGate.enforceFIFO(msg.seq, msg.payload).foreach{
          i =>
            println(s"------------------- controller received message with seq = ${msg.seq} -------------------")
            i.invoke(output)
      }
      Behaviors.same
    }



    //main() starts here:
    output.sendDataToWorker(Suspend(1000).thenDo(PrintString("suspended for 1 s!")))
    output.sendDataToWorker(
      Suspend(1000)
        .thenDo(PrintString("ready to send message to controller!"))
        .thenDo(SendDataToWorker(PrintString("received reply from controller")))
        .thenDo(SendDataToWorker(PrintString("received message from myself")))
        .thenDo(Suspend(300))
        .thenDo(SendControlToWorker(PrintString("received control from myself")))
        .thenDo(SendToController(PrintString("finished routine")))
    )
  }

}