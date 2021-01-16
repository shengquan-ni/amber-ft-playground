package com.example.messages

import com.example.RunnableMessage

case class SendToController(msg:RunnableMessage) extends RunnableMessage(output => output.sendToController(msg))
