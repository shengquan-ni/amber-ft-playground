package com.example.messages

import com.example.RunnableMessage

case class PrintArray() extends RunnableMessage(state => println(s"now array = ${state.arrayState.mkString(",")}"))
