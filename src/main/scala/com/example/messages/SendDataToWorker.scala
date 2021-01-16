package com.example.messages

import com.example.RunnableMessage

case class SendDataToWorker(call:RunnableMessage) extends RunnableMessage(output => output.sendDataToWorker(call))
