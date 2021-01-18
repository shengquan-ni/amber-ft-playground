package com.example.messages

import com.example.RunnableMessage
import com.example.RunnableMessage.StateChangeCall

case class RemoveElementInArray[T](elem:Any) extends RunnableMessage[T](StateChangeCall[T](state => state.arrayState.remove(state.arrayState.indexWhere(i => i == elem))))