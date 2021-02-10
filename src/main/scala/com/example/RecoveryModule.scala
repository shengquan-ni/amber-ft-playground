package com.example

import com.example.RecoveryModule.RecoverableMessage

import scala.collection.mutable

object RecoveryModule{
  case class RecoverableMessage[T](payload:T,seqs:Map[String,Long])

}

class RecoveryModule[T, U](name:String, storageModule:StorageModule[T]) {

  val persistedControlMessages:mutable.Queue[RecoverableMessage[T]] = mutable.Queue(storageModule.load():_*)

  println("recovery: previous controls: \n"+persistedControlMessages.mkString("\n"))
  storageModule.clean()

  val blockedMessages: mutable.Queue[U] = mutable.Queue.empty
  val seqs:mutable.Map[String,Long] = mutable.HashMap.empty

  def feedInData(input:U, sender:String):Seq[Either[T,U]] = {
    if(persistedControlMessages.nonEmpty) {
      if (!persistedControlMessages.head.seqs.contains(sender)) {
        blockedMessages.enqueue(input)
        return Seq.empty
      }
      else if (seqs.contains(sender) && seqs(sender) == persistedControlMessages.head.seqs(sender)) {
        blockedMessages.enqueue(input)
        return Seq.empty
      }
    }
    advanceSeqs(sender)
    Seq(Right(input)) ++ outputControlMessages().map(Left(_))
  }

  def dequeueAllBlockedMessages:Seq[U] = {
    val ret = blockedMessages.toSeq
    blockedMessages.clear()
    ret
  }

  def outputControlMessages(): Seq[T] ={
    val old = persistedControlMessages.isEmpty
    val ret = persistedControlMessages.dequeueWhile(f => f.seqs == seqs).map(x => x.payload).toSeq
    if(old != persistedControlMessages.isEmpty){
      GlobalControl.resendControlMessagesFor(name)
    }
    ret
  }

  private def advanceSeqs(k:String): Unit ={
    if(seqs.contains(k)){
      seqs(k) += 1
    }else{
      seqs(k) = 1
    }
  }

  def persistControlMessage(message:T): Unit ={
    val recoverableMessage = RecoverableMessage(message,seqs.toMap)
    println(s"persisting $recoverableMessage")
    storageModule.persist(recoverableMessage)
  }
  def isRecovering: Boolean = persistedControlMessages.nonEmpty

}
