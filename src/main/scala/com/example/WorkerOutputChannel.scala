package com.example

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.ActorRef
import com.example.ControllerActor.{ControllerMessage, Execute, Log}

class WorkerOutputChannel(controller:ActorRef[ControllerMessage]) {
  var controllerSeq = new AtomicLong()

  def processAndLog(id:Long, cursor:Long): Unit ={
    controller ! Log("worker",controllerSeq.getAndIncrement(), id, cursor)
  }

  def sendToController(content: RunnableMessage[ControllerOutputChannel]): Unit ={
    controller ! Execute("worker", controllerSeq.getAndIncrement(), content)
  }

}
