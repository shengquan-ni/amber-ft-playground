package com.example

import com.example.RecoveryModule.RecoverableMessage
import com.example.WorkerActor.ControlMessage

import scala.collection.mutable

object RecoveryModule{
  case class RecoverableMessage[T](payload:T,seqs:Map[String,Long])

}

class RecoveryModule[T <: ControlMessage, U](name:String, version:Long, fifoGate:mutable.Map[String,OrderingEnforcer[ControlMessage]], storageModule:StorageModule[T]) {

  val persistedControlMessages:mutable.Queue[RecoverableMessage[T]] = mutable.Queue(storageModule.load():_*)

  var recovering: Boolean = version != 0

  println("recovery: previous controls: \n"+persistedControlMessages.mkString("\n"))
  storageModule.clean()
  if(isRecovering){
    if(persistedControlMessages.isEmpty){
      recovering = false
      GlobalControl.resendControlMessagesFor(name)
    }
  }



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
      recovering = false
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
    println(s"now seq = ${seqs.mkString(",")}")
  }

  def persistControlMessage(message:T): Unit ={
    val recoverableMessage = RecoverableMessage(message,seqs.toMap)
    println(s"persisting $recoverableMessage")
    storageModule.persist(recoverableMessage)
  }
  def isRecovering: Boolean = recovering

}
