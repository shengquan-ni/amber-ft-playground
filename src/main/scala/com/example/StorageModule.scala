package com.example

import com.example.RecoveryModule.RecoverableMessage
import com.example.WorkerActor.WorkerMessage

abstract class StorageModule[T] {

  def persist(message:RecoverableMessage[T])

  def load():Seq[RecoverableMessage[T]]

  def beforeSendMessage(message:WorkerMessage)

  def clean()

}
