package com.example.messages

import com.example.RunnableMessage

case class AddElementToArray(elem:Any) extends RunnableMessage(output => output.arrayState.addOne(elem))