package com.example


class RunnableMessage(protected val calls: Iterable[MutableState => Unit]) {

  def this(call: MutableState => Unit){
    this(Iterable(call))
  }

  def invoke(output: MutableState): Unit = {
    calls.foreach{
      call => call(output)
    }
  }

  def thenDo(next:RunnableMessage): RunnableMessage ={
    new RunnableMessage(calls ++ next.calls)
  }

}
