package com.example

import java.util.concurrent.Executors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.example.ControllerActor.ControllerMessage

object WorkerActor {

  sealed trait WorkerMessage

  final case class DataMessage(seq:Long, dataPayload: RunnableMessage) extends WorkerMessage
  final case class ControlMessage(seq:Long, controlPayload: RunnableMessage) extends WorkerMessage

  def apply(parent:ActorRef[ControllerMessage]): Behavior[WorkerMessage] =
    Behaviors.setup(context => new WorkerBehavior(parent, context))

  class WorkerBehavior(parent:ActorRef[ControllerMessage], context: ActorContext[WorkerMessage]) extends AbstractBehavior[WorkerMessage](context) {

    def funToRunnable(fun: () => Unit): Runnable = new Runnable() { def run(): Unit = fun() }

    val output = new MutableState(parent,context.self,context)

    val controlFIFOGate = new OrderingEnforcer[RunnableMessage]
    val dataFIFOGate = new OrderingEnforcer[RunnableMessage]

    val dp = Executors.newSingleThreadExecutor()

    override def onMessage(message: WorkerMessage): Behavior[WorkerMessage] = {
      message match {
        case payload: DataMessage =>
          dataFIFOGate.enforceFIFO(payload.seq, payload.dataPayload).foreach {
            println(s"------------------- worker received data message with seq = ${payload.seq} -------------------")
            i => dp.submit(funToRunnable(() => i.invoke(output)))
          }
        case payload: ControlMessage =>
          controlFIFOGate.enforceFIFO(payload.seq, payload.controlPayload).foreach {
            println(s"------------------- worker received control message with seq = ${payload.seq} -------------------")
            func => func.invoke(output)
          }
      }
      Behaviors.same
    }
  }
}
