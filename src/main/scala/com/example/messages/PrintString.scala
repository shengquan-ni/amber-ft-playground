package com.example.messages

import com.example.RunnableMessage
import com.example.RunnableMessage.StateChangeCall

case class PrintString[T](str:String) extends RunnableMessage[T](StateChangeCall[T](state => println(str)))
