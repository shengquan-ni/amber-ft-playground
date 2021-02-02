package com.example

import java.io.{FileInputStream, ObjectInputStream}
import java.util.concurrent.{CompletableFuture, Executors, LinkedBlockingDeque}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.example.ControllerActor.{ControllerMessage, Log}

import scala.collection.mutable

object WorkerActor {

  sealed trait WorkerMessage extends FIFOMessage

  final case class DataMessage(sender:String, seq:Long, dataPayload: RunnableMessage[WorkerOutputChannel]) extends WorkerMessage
  final case class ControlMessage(sender:String, seq:Long, controlPayload: RunnableMessage[WorkerOutputChannel]) extends WorkerMessage

  def apply(name:String, parent:ActorRef[ControllerMessage], recoveryInfo:mutable.Queue[(WorkerMessage, Long)] = mutable.Queue.empty, stateCheckpoint:MutableState = null): Behavior[WorkerMessage] =
    Behaviors.setup(context => new WorkerBehavior(name, parent, recoveryInfo, context, stateCheckpoint))

  class WorkerBehavior(name:String, parent:ActorRef[ControllerMessage], recoveryInfo:mutable.Queue[(WorkerMessage, Long)], context: ActorContext[WorkerMessage], stateCheckpoint:MutableState = null) extends AbstractBehavior[WorkerMessage](context) {

    def funToRunnable(fun: () => Unit): Runnable = new Runnable() { def run(): Unit = fun() }

    var controlFIFOGate:mutable.AnyRefMap[String, OrderingEnforcer[ControlMessage]] = _
    var dataFIFOGate:mutable.AnyRefMap[String, OrderingEnforcer[DataMessage]] = _

    var state: MutableState =
      if(stateCheckpoint != null){
        controlFIFOGate = stateCheckpoint.controlFIFOGate
        dataFIFOGate = stateCheckpoint.dataFIFOGate
        stateCheckpoint
      }else{
        controlFIFOGate = new mutable.AnyRefMap[String, OrderingEnforcer[ControlMessage]]
        dataFIFOGate = new mutable.AnyRefMap[String, OrderingEnforcer[DataMessage]]
        new MutableState(dataFIFOGate, controlFIFOGate)
      }

    var outputChannel = new WorkerOutputChannel(parent)

    val internalQueue = new LinkedBlockingDeque[Option[RunnableMessage[WorkerOutputChannel]]]
    val dp = Executors.newSingleThreadExecutor()
    var blocker:CompletableFuture[Void] = null
    var isPaused = false

    GlobalControl.workerState = state
    GlobalControl.workerOutput = outputChannel

    dp.submit{
      new Runnable() {
        def run(): Unit = {
          try {
            while(true){
              val msg = internalQueue.take()
              if(msg.isDefined){
                state.dataCursor += 1
                msg.get.invoke(state, outputChannel)
              }
              recover(state.dataCursor)
              if(isPaused){
                blocker = new CompletableFuture[Void]
                blocker.get
              }
            }
          } catch {
            case e: Exception =>
              throw new RuntimeException(e)
          }
        }
      }
    }

    recover(state.dataCursor)

    override def onMessage(message: WorkerMessage): Behavior[WorkerMessage] = {
      processMessage(message)
      Behaviors.same
    }

    def syncWithDPThread(): Unit = {
      isPaused = true
      internalQueue.add(None)
      while(blocker == null || blocker.isDone){}
    }


    def recover(dataCursor:Long): Unit ={
      while(recoveryInfo.nonEmpty && recoveryInfo.head._2 == dataCursor){
        val msg = recoveryInfo.dequeue()
        processMessage(msg._1)
      }
    }

    def processMessage(msg:WorkerMessage): Unit ={
      msg match {
        case payload: DataMessage =>
          OrderingEnforcer.reorderMessage(dataFIFOGate, payload.sender, payload.seq, payload).foreach {
            i =>
              i.foreach{
                m =>
                  println(s"------------------- ${name} received data message with seq = ${m.seq} from ${m.sender} -------------------")
                  internalQueue.add(Some(m.dataPayload))
              }
          }
        case payload: ControlMessage =>
          OrderingEnforcer.reorderMessage(controlFIFOGate, payload.sender, payload.seq, payload).foreach {
            i =>
              i.foreach{
                m =>
                  println(s"------------------- ${name} received control message with seq = ${m.seq} from ${m.sender} -------------------")
                  syncWithDPThread()
                  outputChannel.processAndLog(payload.seq, state.dataCursor)
                  m.controlPayload.invoke(state, outputChannel)
                  blocker.complete(null)
                  isPaused = false
              }
          }
      }
    }
  }
}
