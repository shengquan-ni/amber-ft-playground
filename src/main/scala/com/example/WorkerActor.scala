package com.example

import java.util.concurrent.{CompletableFuture, Executors, LinkedBlockingDeque}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.example.ControllerActor.{ControllerMessage, Log}

import scala.collection.mutable

object WorkerActor {

  sealed trait WorkerMessage

  final case class DataMessage(sender:String, seq:Long, dataPayload: RunnableMessage) extends WorkerMessage
  final case class ControlMessage(sender:String, seq:Long, controlPayload: RunnableMessage) extends WorkerMessage

  def apply(parent:ActorRef[ControllerMessage]): Behavior[WorkerMessage] =
    Behaviors.setup(context => new WorkerBehavior(parent, context))

  class WorkerBehavior(parent:ActorRef[ControllerMessage], context: ActorContext[WorkerMessage]) extends AbstractBehavior[WorkerMessage](context) {

    def funToRunnable(fun: () => Unit): Runnable = new Runnable() { def run(): Unit = fun() }

    var output = new MutableState("worker", parent,context.self,context)

    val controlFIFOGate = new mutable.AnyRefMap[String, OrderingEnforcer[ControlMessage]]
    val dataFIFOGate = new mutable.AnyRefMap[String, OrderingEnforcer[DataMessage]]

    val internalQueue = new LinkedBlockingDeque[Option[RunnableMessage]]
    val dp = Executors.newSingleThreadExecutor()
    var blocker:CompletableFuture[Void] = null
    var isPaused = false
    var dataCursor = 0L

    GlobalControl.workerState = output

    dp.submit{
      new Runnable() {
        def run(): Unit = {
          try {
            while(true){
              val msg = internalQueue.take()
              if(msg.isDefined){
                dataCursor += 1
                msg.get.invoke(output)
              }
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
      message match {
        case payload: DataMessage =>
          OrderingEnforcer.reorderMessage(dataFIFOGate, payload.sender, payload.seq, payload).foreach {
            i =>
              i.foreach{
                m =>
                  println(s"------------------- worker received data message with seq = ${m.seq} from ${m.sender} -------------------")
                  internalQueue.add(Some(m.dataPayload))
              }
          }
        case payload: ControlMessage =>
          OrderingEnforcer.reorderMessage(controlFIFOGate, payload.sender, payload.seq, payload).foreach {
            i =>
              i.foreach{
                m =>
                  println(s"------------------- worker received control message with seq = ${m.seq} from ${m.sender} -------------------")
                  println("ready to pause DP thread")
                  GlobalControl.promptCrash()
                  syncWithDPThread()
                  println("ready to send log to controller")
                  GlobalControl.promptCrash()
                  output.sendLogToController(m.controlPayload, dataCursor)
                  println("ready to execute message")
                  GlobalControl.promptCrash()
                  m.controlPayload.invoke(output)
                  println("executed message")
                  GlobalControl.promptCrash()
              }
          }
      }
      Behaviors.same
    }

    def syncWithDPThread(): Unit = {
      isPaused = true
      internalQueue.add(None)
      while(blocker != null && blocker.isDone){}
    }
  }
}
