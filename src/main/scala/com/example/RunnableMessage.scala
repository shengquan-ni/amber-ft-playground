package com.example
import com.example.RunnableMessage.Call

import scala.io.StdIn.readLine

object RunnableMessage{
  sealed trait Call[T]
  case class StateChangeCall[T](call:MutableState => Unit) extends Call[T]
  case class SendCall[T](call:T => Unit) extends Call[T]

}


class RunnableMessage[T](protected val calls: Iterable[Call[T]]) {

  def this(call: Call[T]){
    this(Iterable(call))
  }

  def invoke(state: MutableState, channel:T): Unit = {
    calls.foreach{
      case RunnableMessage.StateChangeCall(call) => {
        print(s"on ${state.name}: ")
        call(state)
      }
      case RunnableMessage.SendCall(call) => call(channel)
    }
  }

  def thenDo(next:RunnableMessage[T]): RunnableMessage[T] ={
    new RunnableMessage(calls ++ next.calls)
  }

}
