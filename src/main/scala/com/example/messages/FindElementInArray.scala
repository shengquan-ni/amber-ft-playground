package com.example.messages

import com.example.RunnableMessage
import com.example.RunnableMessage.StateChangeCall

case class FindElementInArray[T](elem:Any) extends RunnableMessage[T](StateChangeCall[T](state => {
  state.arrayState.find(i => i == elem) match {
    case None => println(s"$elem not found!")
    case Some(value) => println(s"$elem found!")
  }
}))
