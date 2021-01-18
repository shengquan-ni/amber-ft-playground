package com.example.messages

import com.example.RunnableMessage.SendCall
import com.example.{ControllerOutputChannel, RunnableMessage, WorkerOutputChannel}

case class SendToController(msg:RunnableMessage[ControllerOutputChannel]) extends RunnableMessage[WorkerOutputChannel](SendCall[WorkerOutputChannel](channel => channel.sendToController(msg)))
