package com.example.messages

import com.example.RunnableMessage

case class FindElementInArray(elem:Any) extends RunnableMessage(output => {
  output.arrayState.find(i => i == elem) match {
    case None => println(s"$elem not found!")
    case Some(value) => println(s"$elem found!")
  }
})
