package com.example.messages

import com.example.RunnableMessage.SendCall
import com.example.{ControllerOutputChannel, RunnableMessage, WorkerOutputChannel}

case class SendControlToWorker(call:RunnableMessage[WorkerOutputChannel]) extends RunnableMessage[ControllerOutputChannel](SendCall[ControllerOutputChannel](channel => channel.sendControlToWorker(call)))
