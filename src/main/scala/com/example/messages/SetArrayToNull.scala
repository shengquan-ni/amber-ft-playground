package com.example.messages

import com.example.RunnableMessage
import com.example.RunnableMessage.StateChangeCall

case class SetArrayToNull[T]() extends  RunnableMessage[T](StateChangeCall[T](state => state.arrayState = null))