package com.example.messages

import com.example.RunnableMessage
import com.example.RunnableMessage.StateChangeCall

case class PrintArray[T]() extends RunnableMessage[T](StateChangeCall[T](state => println(s"now array = ${state.arrayState.mkString(",")}")))
