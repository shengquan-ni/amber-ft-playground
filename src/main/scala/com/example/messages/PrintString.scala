package com.example.messages

import com.example.RunnableMessage

case class PrintString(str:String) extends RunnableMessage(output => println(str))
