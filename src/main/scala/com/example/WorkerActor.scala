package com.example

import java.util.concurrent.{CompletableFuture, Executors, LinkedBlockingDeque}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import scala.collection.mutable

object WorkerActor {

  sealed trait WorkerMessage

  final case class ResendControl(to:String, fromSeq:Long = -1) extends WorkerMessage
  final case class ResendData(to:String) extends WorkerMessage
  final case class CleanUp() extends WorkerMessage

  final case class DataInput(sender:String, dataPayload:RunnableMessage)
  final case class DataMessage(sender:String, seq:Long, dataPayload: RunnableMessage) extends WorkerMessage with FIFOMessage
  final case class ControlMessage(sender:String, seq:Long, controlPayload: RunnableMessage) extends WorkerMessage with FIFOMessage

  def apply(name:String, storageModule: StorageModule[ControlMessage], dataToSend:Seq[(RunnableMessage, String)], controlToSend:Seq[(RunnableMessage, String)], recoverVersion:Long = 0L): Behavior[WorkerMessage] =
    Behaviors.setup(context => new WorkerBehavior(name, storageModule, dataToSend, controlToSend,recoverVersion , context))

  class WorkerBehavior(name:String, storageModule: StorageModule[ControlMessage], dataToSend:Seq[(RunnableMessage, String)], controlToSend:Seq[(RunnableMessage, String)],recoverVersion:Long, context: ActorContext[WorkerMessage]) extends AbstractBehavior[WorkerMessage](context) {

    def funToRunnable(fun: () => Unit): Runnable = new Runnable() { def run(): Unit = fun() }

    var state = new MutableState()
    val outputModule:OutputModule = new OutputModule(name,storageModule)

    val controlFIFOGate = new mutable.AnyRefMap[String, OrderingEnforcer[ControlMessage]]
    val dataFIFOGate = new mutable.AnyRefMap[String, OrderingEnforcer[DataMessage]]

    val internalQueue = new LinkedBlockingDeque[Option[DataInput]]
    val dp = Executors.newSingleThreadExecutor()
    var blocker:CompletableFuture[Void] = null
    var isPaused = false
    val recoveryModule = new RecoveryModule[ControlMessage, DataInput](name, storageModule)

    dataToSend.foreach{
      x => outputModule.sendDataTo(x._2, x._1)
    }

    controlToSend.foreach{
      x => outputModule.sendControlTo(x._2, x._1)
    }

    if(recoveryModule.isRecovering){
      recoveryModule.outputControlMessages().foreach{
        x => processMessage(x)
      }
      GlobalControl.resendDataMessagesFor(name)
    }

    dp.submit{
      new Runnable() {
        def run(): Unit = {
          try {
            while(true){
              val msg = internalQueue.take()
              if(msg.isDefined){
                recoveryModule.feedInData(msg.get, msg.get.sender).foreach{
                  case Left(value) =>
                    processMessage(value, true)
                  case Right(value) =>
                    value.dataPayload.invoke(state, outputModule, context)
                }
                recoveryModule.dequeueAllBlockedMessages.foreach{
                  x => internalQueue.add(Option(x))
                }
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
      processMessage(message)
      Behaviors.same
    }

    def syncWithDPThread(fromDPThread:Boolean): Unit = {
      if(!fromDPThread){
        isPaused = true
        internalQueue.add(None)
        while(blocker != null && blocker.isDone){}
      }
    }


    def processMessage(msg:WorkerMessage, fromDPThread:Boolean = false): Unit ={
      val printName = if(recoverVersion!= 0)name+s"(recovered version = ${recoverVersion})" else name
      msg match {
        case ResendControl(to, fromSeq) =>
          outputModule.idMapping(to) = GlobalControl.getRef(to)
          outputModule.sendOldControl(to, fromSeq)
        case ResendData(to) =>
          outputModule.idMapping(to) = GlobalControl.getRef(to)
          outputModule.sendOldData(to)
        case CleanUp() =>
          storageModule.clean()
        case payload: DataMessage =>
          OrderingEnforcer.reorderMessage(dataFIFOGate, payload.sender, payload.seq, payload).foreach {
            i =>
              i.foreach{
                m =>
                  println(s"------------------- ${printName} received data message with seq = ${m.seq} from ${m.sender} -------------------")
                  internalQueue.add(Some(DataInput(payload.sender, m.dataPayload)))
              }
          }
        case payload: ControlMessage =>
          OrderingEnforcer.reorderMessage(controlFIFOGate, payload.sender, payload.seq, payload).foreach {
            i =>
              i.foreach{
                m =>
                  println(s"------------------- ${printName} received control message with seq = ${m.seq} from ${m.sender} -------------------")
                  syncWithDPThread(fromDPThread)
                  recoveryModule.persistControlMessage(payload)
                  m.controlPayload.invoke(state, outputModule, context)
                  if(blocker != null) {
                    blocker.complete(null)
                  }
              }
          }
      }
    }
  }
}
