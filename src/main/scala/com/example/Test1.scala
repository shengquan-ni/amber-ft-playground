package com.example

import com.example.ControllerActor.RecoverWorker
import com.example.messages.{AddElementToArray, Checkpoint, PrintArray, SendControlToWorker, SendDataToWorker, SendToController}

object Test1 extends App{
  EntryPoint{
    output =>
      output.sendControlToWorker(AddElementToArray("1"))
      output.sendDataToWorker(AddElementToArray("2"))
      output.sendControlToWorker(AddElementToArray("3"))
      output.sendDataToWorker(AddElementToArray("4"))
      output.sendControlToWorker(AddElementToArray("5"))
      output.sendControlToWorker(SendToController(AddElementToArray("1 from worker")))
      output.sendDataToWorker(AddElementToArray("6"))
      output.sendControlToWorker(AddElementToArray("7"))
      output.sendControlToWorker(SendToController(AddElementToArray("2 from worker")))
      output.sendDataToWorker(Checkpoint())
      output.sendControlToWorker(AddElementToArray("8"))
      output.sendDataToWorker(AddElementToArray("9"))
      output.sendControlToWorker(AddElementToArray("10"))
      output.sendDataToWorker(AddElementToArray("11"))
      output.sendControlToWorker(PrintArray())
      Thread.sleep(1000)
      GlobalControl.controllerRef ! RecoverWorker("F:\\Amber2.0\\amber-ft-playground\\.\\checkpoints\\checkpoint1.tmp")
      Thread.sleep(10000)
  }

}
