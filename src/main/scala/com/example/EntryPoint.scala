package com.example

import akka.actor.typed.ActorSystem
import com.example.ControllerActor.ControllerMessage

case class EntryPoint(mainFunc: MutableState => Unit){
  val system: ActorSystem[ControllerMessage] = ActorSystem(ControllerActor(mainFunc), "controller")
}
