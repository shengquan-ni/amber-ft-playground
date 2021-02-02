package com.example

import java.util.concurrent.atomic.AtomicLong

import com.example.WorkerActor.{ControlMessage, DataMessage}

import scala.collection.mutable

class MutableState(val dataFIFOGate:mutable.AnyRefMap[String, OrderingEnforcer[DataMessage]],
                   val controlFIFOGate:mutable.AnyRefMap[String, OrderingEnforcer[ControlMessage]],
                   val controllerSeq:AtomicLong) extends Serializable{
  var arrayState: mutable.ArrayBuffer[Any] = mutable.ArrayBuffer.empty
  var dataCursor:Long = 0

}
