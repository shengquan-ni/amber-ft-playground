package com.example

import java.util.concurrent.{CompletableFuture, Executors, LinkedBlockingDeque}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import scala.collection.mutable
import scala.util.Random

object WorkerActor {

  sealed trait WorkerMessage

  final case class PrintStates() extends WorkerMessage
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
    val recoveryModule = new RecoveryModule[ControlMessage, DataInput](name, recoverVersion, controlFIFOGate, storageModule)

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
      Thread.sleep(1000)
      GlobalControl.resendDataMessagesFor(name)
    }

    dp.submit{
      new Runnable() {
        def run(): Unit = {
          try {
            while(true){
              val msg = internalQueue.take()
              if(msg.isDefined){
                var unblockMessages = false
                recoveryModule.feedInData(msg.get, msg.get.sender).foreach{
                  case Left(value) =>
                    processControlMessage(value, true)
                    unblockMessages = true
                  case Right(value) =>
                    value.dataPayload.invoke(state, outputModule, context)
                }
                if(unblockMessages){
                  recoveryModule.dequeueAllBlockedMessages.reverse.foreach{
                    x => internalQueue.addFirst(Option(x))
                  }
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


    def processControlMessage(msg:ControlMessage, fromDPThread:Boolean): Unit ={
      val printName = if(recoverVersion!= 0)name+s"(recovered version = ${recoverVersion})" else name
      println(s"------------------- ${printName} received control message with seq = ${msg.seq} from ${msg.sender} -------------------")
      syncWithDPThread(fromDPThread)
      recoveryModule.persistControlMessage(msg)
      msg.controlPayload.invoke(state, outputModule, context)
      if(blocker != null) {
        blocker.complete(null)
      }
    }


    def processMessage(msg:WorkerMessage): Unit ={
      val printName = if(recoverVersion!= 0)name+s"(recovered version = ${recoverVersion})" else name
      msg match {
        case PrintStates() =>
          println(s"$name: \n fifoControlGate: ${controlFIFOGate.mkString(",")} \n fifoDataGate: ${dataFIFOGate.mkString(",")} \n state: ${state.arrayState.mkString(",")} \n output control seqs: ${outputModule.controlSeqs.mkString(",")} \n output data seqs: ${outputModule.dataSeqs.mkString(",")}")
        case ResendControl(to, fromSeq) =>
          outputModule.idMapping(to) = GlobalControl.getRef(to)
          println("start resending control")
          outputModule.sendOldControl(to, fromSeq)
        case ResendData(to) =>
          outputModule.idMapping(to) = GlobalControl.getRef(to)
          println("start resending data")
          outputModule.sendOldData(to)
        case CleanUp() =>
          storageModule.clean()
        case payload: DataMessage =>
          Thread.sleep(Random.between(10,500))
          OrderingEnforcer.reorderMessage(dataFIFOGate, payload.sender, payload.seq, payload).foreach {
            i =>
              i.foreach{
                m =>
                  println(s"------------------- ${printName} received data message with seq = ${m.seq} from ${m.sender} -------------------")
                  internalQueue.add(Some(DataInput(payload.sender, m.dataPayload)))
              }
          }
        case payload: ControlMessage =>
          Thread.sleep(Random.between(10,500))
          OrderingEnforcer.reorderMessage(controlFIFOGate, payload.sender, payload.seq, payload).foreach {
            i =>
              i.foreach{
                m => processControlMessage(m,false)
              }
          }
      }
    }
  }
}
