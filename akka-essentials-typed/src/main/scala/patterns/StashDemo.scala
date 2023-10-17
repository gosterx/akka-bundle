package patterns

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import utils.ActorSystemEnhancements

import scala.concurrent.duration.DurationInt

object StashDemo {

  trait Command
  case object Open               extends Command
  case object Close              extends Command
  case object Read               extends Command
  case class Write(data: String) extends Command

  object ResourceActor {
    def apply(): Behavior[Command] = closed("init")

    def closed(data: String): Behavior[Command] = Behaviors.withStash(128) { buffer =>
      Behaviors.receive { (context, message) =>
        message match {
          case Open =>
            context.log.info("Opening Resource")
            buffer.unstashAll(open(data))
          case _ =>
            context.log.info(s"Stashing $message because the resource is closed")
            buffer.stash(message) // <- MUTABLE
            Behaviors.same
        }
      }
    }

    def open(data: String): Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case Read =>
          context.log.info(s"I've read $data")
          Behaviors.same
        case Write(newData) =>
          context.log.info(s"I have written $newData")
          open(newData)
        case Close =>
          context.log.info("Closing the resource")
          closed(data)
        case message =>
          context.log.info(s"$message not supported while resource is open")
          Behaviors.same
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val actor = context.spawn(ResourceActor(), "resourceActor")

      actor ! Read
      actor ! Open
      actor ! Open
      actor ! Write("NEW DATA")
      actor ! Write("AGAIN NEW DATA")
      actor ! Read
      actor ! Close
      actor ! Read

      Behaviors.empty
    }

    ActorSystem(userGuardian, "StashDemo").withFiniteLifespan(2.seconds)
  }
}
