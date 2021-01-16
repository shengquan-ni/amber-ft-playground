package com.example.messages

import com.example.RunnableMessage

case class RemoveElementInArray(elem:Any) extends RunnableMessage(output => output.arrayState.remove(output.arrayState.indexWhere(i => i == elem)))