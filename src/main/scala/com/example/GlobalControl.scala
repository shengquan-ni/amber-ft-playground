package com.example


import akka.actor.Kill
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, RecipientRef}
import akka.util.Timeout
import com.example.GuardianActor.{CreateWorker, GuardianMessage, RecoverForWorker}
import com.example.WorkerActor.{CleanUp, ControlMessage, ResendControl, ResendData, WorkerMessage}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


object GlobalControl {


  def SystemActor(): Behaviors.Receive[Any] = {
    Behaviors.receiveMessage { message =>
      Behaviors.same
    }
  }


  private val refMap = mutable.HashMap[String, ActorRef[WorkerMessage]]()

  private var sys: ActorSystem[GuardianMessage] = _

  private var systemActor:RecipientRef[GuardianMessage] = _

  implicit val timeout: Timeout = 3.seconds
  // implicit ActorSystem in scope
  implicit lazy val system: ActorSystem[_] = sys

  // the response callback will be executed on this execution context
  implicit lazy val ec = system.executionContext

  def getRef(name:String):ActorRef[WorkerMessage] = refMap(name)

  def startRecoverFor(name:String): Unit ={
    val ref = systemActor.ask(replyTo = ref => RecoverForWorker(name, ref))
    refMap(name) = Await.result(ref,5.seconds)
  }

  def init(): Unit ={
    sys = ActorSystem[GuardianMessage](GuardianActor(), "ft-system")
    systemActor = sys.ref
  }

  def createWorker(name:String, dataToSend:Seq[(RunnableMessage, String)] = Seq.empty, controlToSend:Seq[(RunnableMessage, String)] = Seq.empty): Unit ={
    val ref = systemActor.ask(replyTo = ref => CreateWorker(name, dataToSend, controlToSend, ref))
    refMap(name) = Await.result(ref,5.seconds)
  }

  def shutdown(): Unit ={
    refMap.values.foreach{
      x => x ! CleanUp()
    }
    sys.terminate()
    sys.getWhenTerminated.toCompletableFuture.get()
  }

  def resendDataMessagesFor(name:String): Unit ={
    refMap.values.foreach{
      x => x ! ResendData(name)
    }
  }

  def resendControlMessagesFor(name:String): Unit ={
    refMap.values.foreach{
      x => x ! ResendControl(name)
    }
  }

}
