package com.example

import com.example.messages.{AddElementToArray, PrintArray, SendData}


object Test1 extends App{
  GlobalControl.init()
  GlobalControl.createWorker("B")
  GlobalControl.createWorker("A", Seq(
    (AddElementToArray(1),"B"),
    (AddElementToArray(2),"B"),
    (PrintArray(),"B")
  ),Seq(
    (SendData("A", AddElementToArray(3)), "B"),
      (SendData("A", AddElementToArray(4)), "B"),
      (SendData("A", AddElementToArray(5)), "B"),
      (SendData("A", AddElementToArray(6)), "B"),
    (SendData("A", PrintArray()), "B"),
  ))
  GlobalControl.createWorker("C", Seq(
    (AddElementToArray(1),"B"),
    (AddElementToArray(2),"B"),
    (PrintArray(),"B")
  ),Seq(
    (SendData("C", AddElementToArray(3)), "B"),
    (SendData("C", AddElementToArray(4)), "B"),
    (SendData("C", AddElementToArray(5)), "B"),
    (SendData("C", AddElementToArray(6)), "B"),
    (SendData("C", PrintArray()), "B"),
  ))
  Thread.sleep(1000)
  GlobalControl.startRecoverFor("B")
  Thread.sleep(10000)
  GlobalControl.shutdown()
}
