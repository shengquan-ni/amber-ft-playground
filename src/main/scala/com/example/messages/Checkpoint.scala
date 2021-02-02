package com.example.messages

import java.io.{FileOutputStream, ObjectOutputStream}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.example.RunnableMessage
import com.example.RunnableMessage.StateChangeCall

case class Checkpoint[T]() extends RunnableMessage[T](StateChangeCall[T](state => {
  println("start checkpointing")
  val fileName = "./checkpoints/checkpoint1.tmp"//s"./checkpoints/${DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now)}.tmp"
  val file = new java.io.File(fileName)
  if(!file.exists()) {
    val result = file.createNewFile()
    println(s"create file: $result")
  }
  val fos = new FileOutputStream(file)
  val oos = new ObjectOutputStream(fos)
  oos.writeObject(state)
  oos.close()
  println(s"checkpointed at ${file.getAbsolutePath}")
}))
