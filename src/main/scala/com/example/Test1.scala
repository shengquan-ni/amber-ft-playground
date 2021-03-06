package com.example

import com.example.messages.{AddElementToArray, PrintArray, SendControlToWorker, SendDataToWorker, SendToController}

object Test1 extends App{
  EntryPoint{
    output =>
      output.sendControlToWorker(
        AddElementToArray("1")
          .thenDo(SendToController(
            AddElementToArray("2")
              .thenDo(SendControlToWorker(AddElementToArray("3")
                .thenDo(PrintArray())
              ))
          ))
      )
      output.sendControlToWorker(AddElementToArray("4"))
      output.sendControlToWorker(PrintArray())
  }

}
