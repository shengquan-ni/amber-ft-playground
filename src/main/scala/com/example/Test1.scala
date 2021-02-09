package com.example

import com.example.ControllerActor.RecoverWorker
import com.example.messages.{AddElementToArray, Checkpoint, PrintArray, SendControlToWorker, SendDataToWorker, SendToController}

import scala.util.Random

object Test1 extends App{
  EntryPoint{
    output =>
      output.sendControlToWorker(AddElementToArray("c1"))
      output.sendDataToWorker(AddElementToArray("d1"))
      output.sendControlToWorker(AddElementToArray("c2"))
      output.sendDataToWorker(AddElementToArray("d2"))
      output.sendControlToWorker(AddElementToArray("c3"))
      output.sendControlToWorker(SendToController(AddElementToArray("1 from worker")))
      output.sendDataToWorker(AddElementToArray("d3"))
      output.sendControlToWorker(AddElementToArray("c4"))
      output.sendControlToWorker(SendToController(AddElementToArray("2 from worker")))
      output.sendDataToWorker(Checkpoint())
      output.sendControlToWorker(AddElementToArray("c5"))
      output.sendDataToWorker(AddElementToArray("d4"))
      output.sendControlToWorker(AddElementToArray("c6"))
      output.sendDataToWorker(AddElementToArray("d5"))
      output.sendControlToWorker(PrintArray())
      Thread.sleep(Random.between(1,30))
      println(">>>>>>>>>>>>>>>>>>>>>>>>> start recovery >>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
      GlobalControl.controllerRef ! RecoverWorker("./checkpoints/checkpoint1.tmp")
      Thread.sleep(10000)
  }

}
