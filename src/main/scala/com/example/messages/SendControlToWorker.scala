package com.example.messages

import com.example.RunnableMessage

case class SendControlToWorker(call:RunnableMessage) extends RunnableMessage(output => output.sendControlToWorker(call))
