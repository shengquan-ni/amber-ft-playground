package com.example

import java.util.concurrent.{CompletableFuture, Executors, LinkedBlockingDeque}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.example.ControllerActor.{ControllerMessage, Log}
import com.twitter.util.{Future, Promise}

import scala.collection.mutable

object WorkerActor {

  sealed trait WorkerMessage extends FIFOMessage

  final case class DataMessage(sender:String, seq:Long, dataPayload: RunnableMessage[WorkerOutputChannel]) extends WorkerMessage
  final case class ControlMessage(sender:String, seq:Long, controlPayload: RunnableMessage[WorkerOutputChannel]) extends WorkerMessage

  def apply(name:String, parent:ActorRef[ControllerMessage], recoveryInfo:mutable.Queue[(WorkerMessage, Long)] = mutable.Queue.empty): Behavior[WorkerMessage] =
    Behaviors.setup(context => new WorkerBehavior(name, parent, recoveryInfo, context))

  class WorkerBehavior(name:String, parent:ActorRef[ControllerMessage], recoveryInfo:mutable.Queue[(WorkerMessage, Long)], context: ActorContext[WorkerMessage]) extends AbstractBehavior[WorkerMessage](context) {

    def funToRunnable(fun: () => Unit): Runnable = new Runnable() { def run(): Unit = fun() }

    var state = new MutableState("worker")
    var outputChannel = new WorkerOutputChannel(parent)

    val controlFIFOGate = new mutable.AnyRefMap[String, OrderingEnforcer[ControlMessage]]
    val dataFIFOGate = new mutable.AnyRefMap[String, OrderingEnforcer[DataMessage]]

    val internalQueue = new LinkedBlockingDeque[Option[RunnableMessage[WorkerOutputChannel]]]
    val dp = Executors.newSingleThreadExecutor()
    var blocker:CompletableFuture[Void] = null
    var rootPausePromise:Promise[Unit] = null
    var pauseFuture:Future[Unit] = null
    def isPaused: Boolean = rootPausePromise != null && !rootPausePromise.isDefined
    var dataCursor = 0L
    var needRelease = true
    var finishedExecutingControl = false

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
              println("dp thread looped once")
              recover(dataCursor)
              if(isPaused){
                blocker = new CompletableFuture[Void]()
                rootPausePromise.setValue()
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

    def syncWithDPThread(): Future[Unit] = {
      if(isPaused){
        //dp thread paused or pausing
        //in this case, returning pausePromise is safe
        //if pausePromise is already fulfilled, onSuccess call will be invoked directly
        //if pausePromise is not fulfilled, onSuccess call will be invoked in dp thread
        println("syncWithDPThread: dp thread paused")
        pauseFuture
      }else{
        // dp thread running
        println("syncWithDPThread: dp thread running, pause it")
        rootPausePromise = new Promise[Unit]()
        internalQueue.add(None)
        rootPausePromise
      }
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
//                  println("ready to pause DP thread")
//                  GlobalControl.promptCrash()
                  pauseFuture = syncWithDPThread().map{
                    x =>
                      println(s"control execution: dp thread process control with seq = ${m.seq} on ${Thread.currentThread().getName}")
                      // println("ready to send log to controller")
                      // GlobalControl.promptCrash()
                      outputChannel.processAndLog(m.seq, dataCursor)
                      // println("ready to execute message")
                      // GlobalControl.promptCrash()
                      m.controlPayload.invoke(state, outputChannel)
                      if(needRelease) {
                        if (!blocker.isDone) {
                          blocker.complete(null)
                        }
                        println(s"control execution: dp thread released on ${Thread.currentThread().getName}")
                      }
//                      println("executed message")
//                      GlobalControl.promptCrash()
                  }
              }
          }
      }
    }
  }
}
