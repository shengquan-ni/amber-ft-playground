package com.example.messages

import com.example.RunnableMessage
import com.example.RunnableMessage.SendCall

class SendControl(to:String, msg:RunnableMessage) extends RunnableMessage(SendCall(channel => channel.sendControlTo(to, msg)))
