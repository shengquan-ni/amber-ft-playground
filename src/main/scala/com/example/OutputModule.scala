package com.example

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.ActorRef
import com.example.WorkerActor.{ControlMessage, DataMessage, WorkerMessage}

import scala.collection.mutable

class OutputModule(senderID:String, storageModule: StorageModule[_]) {

  val controlSeqs: mutable.Map[String, Long] = mutable.HashMap[String, Long]()
  val dataSeqs: mutable.Map[String, Long] =  mutable.HashMap[String, Long]()

  val idMapping: mutable.Map[String, ActorRef[WorkerMessage]] = mutable.HashMap[String, ActorRef[WorkerMessage]]()


  private val sentControls = mutable.Map[String, mutable.ArrayBuffer[ControlMessage]]()
  private val sentDatas = mutable.Map[String, mutable.ArrayBuffer[DataMessage]]()

  def sendControlTo(receiver:String, message: RunnableMessage): Unit ={
    getMapping(receiver)
    if(!controlSeqs.contains(receiver)){
      controlSeqs(receiver) = 0
    }
    if(!sentControls.contains(receiver)){
      sentControls(receiver) = mutable.ArrayBuffer.empty
    }
    val controlMsg = ControlMessage(senderID, controlSeqs(receiver), message)
    controlSeqs(receiver) +=1
    sentControls(receiver).append(controlMsg)
    storageModule.beforeSendMessage(controlMsg)
    idMapping(receiver) ! controlMsg
  }

  def sendDataTo(receiver:String, message: RunnableMessage): Unit ={
    getMapping(receiver)
    if(!dataSeqs.contains(receiver)){
      dataSeqs(receiver) = 0
    }
    if(!sentDatas.contains(receiver)){
      sentDatas(receiver) = mutable.ArrayBuffer.empty
    }
    val dataMsg = DataMessage(senderID, dataSeqs(receiver), message)
    dataSeqs(receiver) +=1
    sentDatas(receiver).append(dataMsg)
    storageModule.beforeSendMessage(dataMsg)
    idMapping(receiver) ! dataMsg
  }


  def sendOldData(to:String): Unit ={
    if(sentDatas.contains(to)){
      sentDatas(to).foreach{
        msg => idMapping(to) ! msg
      }
    }
  }


  def sendOldControl(to:String, from:Long):Unit = {
    if(sentControls.contains(to)){
      sentControls(to).drop(from.toInt).foreach{
        msg => idMapping(to) ! msg
      }
    }
  }


  def getMapping(name:String): Unit = {
    idMapping(name) = GlobalControl.getRef(name)
  }


}
