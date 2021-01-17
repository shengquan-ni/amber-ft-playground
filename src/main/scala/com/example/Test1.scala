package com.example

import com.example.messages.{AddElementToArray, PrintArray, SendControlToWorker, SendDataToWorker}

object Test1 extends App{
  EntryPoint{
    output =>
      output.sendControlToWorker(
        AddElementToArray("1")
          .thenDo(SendControlToWorker(AddElementToArray("2")))
          .thenDo(SendDataToWorker(AddElementToArray("3")))
      )
      output.sendControlToWorker(AddElementToArray("4"))
      output.sendControlToWorker(PrintArray())
  }

}
