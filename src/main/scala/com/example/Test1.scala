package com.example

import com.example.messages.{AddElementToArray, PrintArray, SendData, SendControl}


object Test1 extends App{
  GlobalControl.init()
  GlobalControl.createWorker("B")
  GlobalControl.createWorker("A", Seq(
    (AddElementToArray(1),"B"),
    (AddElementToArray(2),"B"),
    (AddElementToArray(3),"B"),
    (AddElementToArray(4),"B"),
    (AddElementToArray(5),"B"),
    (PrintArray(),"B")
  ),Seq(
    (SendControl("A", AddElementToArray(3)), "B"),
      (SendControl("B", AddElementToArray(4)), "B"),
      (SendControl("A", AddElementToArray(5)), "B"),
      (SendControl("B", AddElementToArray(6)), "B")
  ))
  GlobalControl.createWorker("C", Seq(
    (AddElementToArray(10),"B"),
    (AddElementToArray(11),"B"),
    (PrintArray(),"B"),
    (PrintArray(),"A")
  ),Seq(
    (SendControl("C", AddElementToArray(3)), "B"),
    (SendControl("A", AddElementToArray(4)), "A"),
    (SendControl("A", AddElementToArray(5)), "B"),
    (SendControl("A", AddElementToArray(6)), "B")
  ))
  Thread.sleep(3000)
  GlobalControl.startRecoverFor("B")
  Thread.sleep(30000)
  GlobalControl.shutdown()
}
