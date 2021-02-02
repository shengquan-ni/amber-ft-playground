package com.example

import java.util.concurrent.{CompletableFuture, Executors}

import akka.actor.typed.ActorRef
import com.example.ControllerActor.{ControllerMessage, Log, RecoverWorker}

import scala.collection.mutable
import scala.io.StdIn.readLine

object GlobalControl {

  var controllerRef:ActorRef[ControllerMessage] = _
  var controllerState:MutableState = _
  var workerState:MutableState = _
  var controllerOutput:ControllerOutputChannel = _
  var workerOutput:WorkerOutputChannel = _

  def promptCrash(): Unit ={
    readLine("do you want to simulate a crash on worker? (y/N)").toLowerCase match{
      case "y" =>
        printState(controllerState, "controller")
        printState(workerState, "worker")
        println("controller Log: ")
        println(s"[${controllerOutput.logs.mkString("\n")}]")
        promptRecovery()
      case other =>
        //skip
    }
  }

  def printState(state:MutableState, name:String):Unit ={
    println(s"$name state: \n" +
      s"arrayState = ${state.arrayState.mkString(",")}\n"
    )
  }


  def promptRecovery(): Unit ={
    readLine("do you want to recover worker? (Y/n)").toLowerCase match{
      case "n" =>
        //skip
      case other =>
        readLine("enter checkpoint file path if you want to recover from checkpoint.").toLowerCase match{
          case path =>
            controllerRef ! RecoverWorker(path)
        }
    }
  }

}
