package com.example.messages

import com.example.RunnableMessage
import com.example.RunnableMessage.StateChangeCall

case class ClearArray[T]() extends RunnableMessage[T](StateChangeCall[T](state => state.arrayState.clear()))
