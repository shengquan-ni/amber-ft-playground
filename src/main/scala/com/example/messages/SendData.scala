package com.example.messages

import com.example.RunnableMessage
import com.example.RunnableMessage.SendCall

case class SendData(to:String, msg:RunnableMessage) extends RunnableMessage(SendCall(channel => channel.sendDataTo(to, msg)))
