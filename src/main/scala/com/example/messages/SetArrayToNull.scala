package com.example.messages

import com.example.RunnableMessage

case class SetArrayToNull() extends RunnableMessage(output => output.arrayState = null)