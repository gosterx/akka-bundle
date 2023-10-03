package part2actors

import akka.actor.typed.{ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors

object StoppingActors {
  object SensitiveActor {
    def apply(): Behavior[String] = Behaviors
      .receive[String] { (context, message) =>
        context.log.info(s"Received: $message")
        if (message == "you are ugly")
          Behaviors.stopped
        else
          Behaviors.same
      }
      .receiveSignal {
        case (context, PostStop) =>
          // clean up resources
          context.log.info("I'm stopping")
          Behaviors.same // not used anymore
      }
  }

  def main(args: Array[String]): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val sensitiveActor = context.spawn(SensitiveActor(), "sensitive")

      sensitiveActor ! "Hi"
      sensitiveActor ! "Hi2"
      sensitiveActor ! "you are ugly"
      sensitiveActor ! "you are ugly"

      Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "stopping")
    Thread.sleep(1000)
    system.terminate()
  }
}
