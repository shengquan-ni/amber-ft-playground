package com.example

import java.util.concurrent.{CompletableFuture, Executors}

import com.example.ControllerActor.Log

import scala.collection.mutable
import scala.io.StdIn.readLine

object GlobalControl {

  var controllerState:MutableState = _
  var workerState:MutableState = _
  var controllerLog:mutable.Queue[Log] = _
  var workerCrashed = false

  def promptCrash(): Unit ={
    readLine("do you want to simulate a crash on worker? (y/N)").toLowerCase match{
      case "y" =>
        workerCrashed = true
        printState(controllerState, "controller")
        printState(workerState, "worker")
        println("controller Log: ")
        println(s"[${controllerLog.mkString("\n")}]")
        promptRecovery()
        throw new RuntimeException("worker crashed")
      case other =>
        //skip
    }
  }

  def printState(state:MutableState, name:String):Unit ={
    println(s"$name state: \n" +
      s"arrayState = ${state.arrayState.mkString(",")}\n" +
      s"out seq for data to worker = ${state.dataSeq}\n" +
      s"out seq for control to worker = ${state.controlSeq}\n" +
      s"out seq for controller = ${state.controllerSeq}\n"
    )
  }


  def promptRecovery(): Unit ={
    readLine("do you want to recover worker? (Y/n)").toLowerCase match{
      case "n" =>
        //skip
      case other =>
        println("triggering recovery")
        workerCrashed = false
    }
  }

}
