package com.example
import akka.actor.typed.scaladsl.ActorContext
import com.example.RunnableMessage.Call
import com.example.WorkerActor.WorkerMessage

import scala.io.StdIn.readLine

object RunnableMessage{
  sealed trait Call
  case class ActorCall(call: ActorContext[WorkerMessage] => Unit) extends Call
  case class StateChangeCall(call:MutableState => Unit) extends Call
  case class SendCall(call:OutputModule => Unit) extends Call

}


class RunnableMessage(protected val calls: Iterable[Call]) {

  def this(call: Call){
    this(Iterable(call))
  }

  def invoke(state: MutableState, channel:OutputModule, ctx:ActorContext[WorkerMessage]): Unit = {
    calls.foreach{
      case RunnableMessage.StateChangeCall(call) => call(state)
      case RunnableMessage.SendCall(call) => call(channel)
      case RunnableMessage.ActorCall(call) => call(ctx)
    }
  }

  def thenDo(next:RunnableMessage): RunnableMessage ={
    new RunnableMessage(calls ++ next.calls)
  }

}
