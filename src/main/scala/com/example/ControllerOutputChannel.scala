package com.example

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.ActorRef
import com.example.ControllerActor.Log
import com.example.WorkerActor.{ControlMessage, DataMessage, WorkerMessage}

import scala.collection.mutable

class ControllerOutputChannel(var worker:ActorRef[WorkerMessage]) {

  var controlSeq = new AtomicLong()
  val logs:mutable.Queue[(WorkerMessage, Long)] = mutable.Queue.empty
  var unLoggedMessages = new mutable.LongMap[WorkerMessage]()
  var dataMessages = new mutable.Queue[WorkerMessage]()

  def sendDataToWorker(content:RunnableMessage[WorkerOutputChannel]): Unit ={
    val seq = dataMessages.size
    val msg = DataMessage("controller", seq, content)
    dataMessages.enqueue(msg)
    worker ! msg
  }

  def sendControlToWorker(content:RunnableMessage[WorkerOutputChannel]): Unit ={
    val seq = controlSeq.getAndIncrement()
    val msg = ControlMessage("controller", seq, content)
    unLoggedMessages(seq) = msg
    worker ! msg
  }

  def logControl(id:Long, dataCursor:Long): Unit ={
    if(unLoggedMessages.contains(id)){
      logs.enqueue((unLoggedMessages(id), dataCursor))
      unLoggedMessages.remove(id)
    }else{
      assert(logs.exists(m => m._1.seq == id && m._2 == dataCursor))
    }
  }


  def resendDataMessages(dataCursor:Long): Unit ={
    dataMessages.drop(dataCursor.toInt).foreach{
      msg =>
        worker ! msg
    }
  }

  def resendUnLoggedControlMessages(): Unit ={
    unLoggedMessages.values.foreach{
      msg =>
        worker ! msg
    }
  }

}
