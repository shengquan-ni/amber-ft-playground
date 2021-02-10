package com.example.messages

import com.example.RunnableMessage
import com.example.RunnableMessage.StateChangeCall

case class PrintArray() extends RunnableMessage(StateChangeCall(state => println(s"now array = ${state.arrayState.mkString(",")}")))
