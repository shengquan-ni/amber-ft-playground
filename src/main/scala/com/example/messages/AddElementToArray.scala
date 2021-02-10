package com.example.messages

import com.example.RunnableMessage
import com.example.RunnableMessage.StateChangeCall

case class AddElementToArray(elem:Any) extends RunnableMessage(StateChangeCall(state => state.arrayState.addOne(elem)))