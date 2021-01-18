package com.example

import java.util.concurrent.{CompletableFuture, Executors, LinkedBlockingDeque}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.example.ControllerActor.{ControllerMessage, Log}

import scala.collection.mutable

object WorkerActor {

  sealed trait WorkerMessage extends FIFOMessage

  final case class DataMessage(sender:String, seq:Long, dataPayload: RunnableMessage[WorkerOutputChannel]) extends WorkerMessage
  final case class ControlMessage(sender:String, seq:Long, controlPayload: RunnableMessage[WorkerOutputChannel]) extends WorkerMessage

  def apply(name:String, parent:ActorRef[ControllerMessage], recoveryInfo:mutable.Queue[(WorkerMessage, Long)] = mutable.Queue.empty): Behavior[WorkerMessage] =
    Behaviors.setup(context => new WorkerBehavior(name, parent, recoveryInfo, context))

  class WorkerBehavior(name:String, parent:ActorRef[ControllerMessage], recoveryInfo:mutable.Queue[(WorkerMessage, Long)], context: ActorContext[WorkerMessage]) extends AbstractBehavior[WorkerMessage](context) {

    def funToRunnable(fun: () => Unit): Runnable = new Runnable() { def run(): Unit = fun() }

    var state = new MutableState()
    var outputChannel = new WorkerOutputChannel(parent)

    val controlFIFOGate = new mutable.AnyRefMap[String, OrderingEnforcer[ControlMessage]]
    val dataFIFOGate = new mutable.AnyRefMap[String, OrderingEnforcer[DataMessage]]

    val internalQueue = new LinkedBlockingDeque[Option[RunnableMessage[WorkerOutputChannel]]]
    val dp = Executors.newSingleThreadExecutor()
    var blocker:CompletableFuture[Void] = null
    var isPaused = false
    var dataCursor = 0L

    GlobalControl.workerState = state
    GlobalControl.workerOutput = outputChannel

    recover(0)

    dp.submit{
      new Runnable() {
        def run(): Unit = {
          try {
            while(true){
              val msg = internalQueue.take()
              if(msg.isDefined){
                dataCursor += 1
                msg.get.invoke(state, outputChannel)
              }
              recover(dataCursor)
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

    override def onMessage(message: WorkerMessage): Behavior[WorkerMessage] = {
      processMessage(message)
      Behaviors.same
    }

    def syncWithDPThread(): Unit = {
      isPaused = true
      internalQueue.add(None)
      while(blocker != null && blocker.isDone){}
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
                  println("ready to pause DP thread")
                  GlobalControl.promptCrash()
                  syncWithDPThread()
                  println("ready to send log to controller")
                  GlobalControl.promptCrash()
                  outputChannel.processAndLog(payload.seq, dataCursor)
                  println("ready to execute message")
                  GlobalControl.promptCrash()
                  m.controlPayload.invoke(state, outputChannel)
                  println("executed message")
                  GlobalControl.promptCrash()
              }
          }
      }
    }
  }
}
