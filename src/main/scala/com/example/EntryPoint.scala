package com.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.ActorContext
import com.example.ControllerActor.ControllerMessage

case class EntryPoint(mainFunc: ControllerOutputChannel => Unit){
  val system: ActorSystem[ControllerMessage] = ActorSystem(ControllerActor(mainFunc), "controller")
}
