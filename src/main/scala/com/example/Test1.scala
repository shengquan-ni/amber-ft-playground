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
      (SendData("B", AddElementToArray("ex1")), "B"),
      (SendData("A", AddElementToArray(5)), "B"),
      (SendControl("B", AddElementToArray("ex2")), "B")
  ))
  GlobalControl.createWorker("C", Seq(
    (AddElementToArray(6),"B"),
    (AddElementToArray(7),"B"),
    (AddElementToArray(8),"B"),
    (AddElementToArray(9),"B"),
    (AddElementToArray(10),"B"),
    (AddElementToArray(11),"B"),
    (PrintArray(),"B"),
    (PrintArray(),"A")
  ),Seq(
    (SendData("C", AddElementToArray(3)), "B"),
    (SendData("A", AddElementToArray(4)), "A"),
    (SendData("A", AddElementToArray(5)), "B"),
    (SendControl("A", AddElementToArray(6)), "B")
  ))

  Thread.sleep(5000)
  GlobalControl.printStates()
  GlobalControl.startRecoverFor("B")
  Thread.sleep(5000)
  GlobalControl.printStates()
  GlobalControl.startRecoverFor("A")
  Thread.sleep(5000)
  GlobalControl.printStates()
  GlobalControl.startRecoverFor("C")
  Thread.sleep(30000)
  GlobalControl.printStates()
  Thread.sleep(5000)
  GlobalControl.shutdown()
}
