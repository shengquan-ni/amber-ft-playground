package com.example.messages

import com.example.RunnableMessage

case class ClearArray() extends RunnableMessage(output => output.arrayState.clear())
