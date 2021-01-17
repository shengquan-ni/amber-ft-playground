package com.example

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import com.example.ControllerActor.{ControllerMessage, Execute, Log}
import com.example.WorkerActor.{ControlMessage, DataMessage, WorkerMessage}

import scala.collection.mutable

class MutableState(myID:String, controllerRef:ActorRef[ControllerMessage], workerRef:ActorRef[WorkerMessage], context: ActorContext[_]){

  var arrayState: mutable.ArrayBuffer[Any] = mutable.ArrayBuffer.empty
  var controlSeq = new AtomicLong()
  var dataSeq = new AtomicLong()
  var controllerSeq = new AtomicLong()

  def sendDataToWorker(content:RunnableMessage): Unit ={
    workerRef ! DataMessage(myID, dataSeq.getAndIncrement(), content)
  }

  def sendControlToWorker(content:RunnableMessage): Unit ={
    workerRef ! ControlMessage(myID, controlSeq.getAndIncrement(), content)
  }

  def sendToController(content: RunnableMessage): Unit ={
    controllerRef ! Execute(myID, controllerSeq.getAndIncrement(), content)
  }

  def sendLogToController(content: RunnableMessage, dataCursor:Long): Unit ={
    controllerRef ! Log(myID, controllerSeq.getAndIncrement(), content, dataCursor)
  }
}
