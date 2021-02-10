package com.example

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import com.example.GuardianActor.GuardianMessage
import com.example.WorkerActor.WorkerMessage

import scala.collection.mutable

object GuardianActor {

  sealed trait GuardianMessage

  case class CreateWorker(name:String, dataToSend:Seq[(RunnableMessage, String)], controlToSend:Seq[(RunnableMessage, String)], ref:ActorRef[ActorRef[WorkerMessage]]) extends GuardianMessage
  case class RecoverForWorker(name:String, ref:ActorRef[ActorRef[WorkerMessage]]) extends GuardianMessage

  def apply(): Behavior[GuardianMessage] =
    Behaviors.setup[GuardianMessage](context => new GuardianActor(context))
}

class GuardianActor(context: ActorContext[GuardianMessage]) extends AbstractBehavior[GuardianMessage](context) {

  private val recoveredVersion = mutable.HashMap[String, Long]()
  private val messageFromOutside = mutable.HashMap[String, (Seq[(RunnableMessage, String)],Seq[(RunnableMessage, String)])]()

  context.log.info("started")

  override def onMessage(msg: GuardianMessage): Behavior[GuardianMessage] = {
    msg match{
      case GuardianActor.CreateWorker(name,dataToSend, controlToSend, ref) =>
        if(!recoveredVersion.contains(name)){
          recoveredVersion(name) = 0
        }
        val printName = if(recoveredVersion(name)!= 0)name+s"(recovered version = ${recoveredVersion(name)})" else name
        val newRef = context.spawnAnonymous(WorkerActor(printName,new LocalDiskStorage(printName),dataToSend,controlToSend))
        messageFromOutside(name) = (dataToSend, controlToSend)
        ref ! newRef
      case GuardianActor.RecoverForWorker(name, ref) =>
        context.stop(GlobalControl.getRef(name))
        recoveredVersion(name)+=1
        val (d,c) = messageFromOutside(name)
        val newRef = context.spawnAnonymous(WorkerActor(name,new LocalDiskStorage(name),d , c))
        ref ! newRef
    }
    Behaviors.same
  }

  override def onSignal: PartialFunction[Signal, Behavior[GuardianMessage]] = {
    case PostStop =>
      context.log.info("stopped")
      this
  }
}
