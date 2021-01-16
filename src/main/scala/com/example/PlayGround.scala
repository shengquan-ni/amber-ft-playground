//#full-example
package com.example


import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.example.ControllerActor.ControllerMessage

//#main-class
object PlayGround extends App {
  val system: ActorSystem[ControllerMessage] = ActorSystem(ControllerActor(), "controller")
}
//#main-class
//#full-example
