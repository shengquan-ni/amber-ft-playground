package com.example

import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import com.esotericsoftware.kryo.KryoException
import com.example.WorkerActor.{ControlMessage, WorkerMessage}
import com.esotericsoftware.kryo.io.{Input, Output}
import com.example.RecoveryModule.RecoverableMessage
import com.twitter.chill.ScalaKryoInstantiator

import scala.collection.mutable

class LocalDiskStorage(name:String) extends StorageModule[ControlMessage] {

  val filePath: Path = Paths.get(s"./logs/$name.logfile")
  Files.createDirectories(filePath.getParent)
  private val kryoInit = new ScalaKryoInstantiator
  kryoInit.setRegistrationRequired(false)
  private val kryo = kryoInit.newKryo()
  private lazy val controlSerializer = new Output(
    Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
  )

  override def persist(message: RecoveryModule.RecoverableMessage[ControlMessage]): Unit = {
    // write message to disk
    try {
      kryo.writeObject(controlSerializer, message)
    }
    controlSerializer.flush()
  }

  override def load(): Seq[RecoveryModule.RecoverableMessage[ControlMessage]] = {
    // read file from disk
    if(!Files.exists(filePath)){
      return Seq.empty
    }
    val input = new Input(Files.newInputStream(filePath))
    var flag = true
    val buf = mutable.ArrayBuffer.empty[RecoverableMessage[ControlMessage]]
    while (flag) {
      try {
        val message = kryo.readObject(input, classOf[RecoverableMessage[ControlMessage]])
        buf.append(message)
      } catch {
        case e: KryoException =>
          input.close()
          flag = false
      }
    }
    buf.toSeq
  }

  override def beforeSendMessage(message: WorkerMessage): Unit = {
    //do nothing
  }

  override def clean(): Unit ={
    //delete file
    if(Files.exists(filePath)){
      Files.delete(filePath)
    }
  }
}
