package com.example.messages

import com.example.RunnableMessage

case class Suspend(mills:Long) extends RunnableMessage(output => Thread.sleep(mills))
