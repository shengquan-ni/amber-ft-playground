package com.example.messages

import com.example.RunnableMessage
import com.example.RunnableMessage.StateChangeCall

case class AddElementToArray[T](elem:Any) extends RunnableMessage[T](StateChangeCall[T](state => state.arrayState.addOne(elem)))