package part4infra

import akka.actor.Cancellable
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import utils._

import scala.concurrent.duration.DurationInt

object Schedulers {

  object LoggerActor {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(s"[${context.self.path}] Received: $message")
      Behaviors.same
    }
  }

  def demoScheduler(): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val loggerActor = context.spawn(LoggerActor(), "loggerActor")

      context.log.info("[system] System starting")
      context.scheduleOnce(1.second, loggerActor, "reminder")

      Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "demoScheduler")

    import system.executionContext
    system.scheduler.scheduleOnce(2.seconds, () => system.terminate())
  }

  // timeout pattern
  def demoActorWithTimeout(): Unit = {
    val timeoutActor: Behavior[String] = Behaviors.receive { (context, message) =>
      val schedule = context.scheduleOnce(1.second, context.self, "timeout")

      message match {
        case "timeout" =>
          context.log.info("Stopping")
          Behaviors.stopped
        case _ =>
          context.log.info(s"Received: $message")
          schedule.cancel()
          Behaviors.same
      }
    }

    val system = ActorSystem(timeoutActor, "timeoutDemo")

    system ! "trigger"
    Thread.sleep(2000)
    system ! "are u there?"
  }

  object ResettingTimeoutActor {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(s"Received: $message")
      resettingTimeout(context.scheduleOnce(1.second, context.self, "timeout"))
    }

    def resettingTimeout(schedule: Cancellable): Behavior[String] = Behaviors.receive { (context, message) =>
      message match {
        case "timeout" =>
          context.log.info("Stopping")
          Behaviors.stopped
        case _ =>
          context.log.info(s"Received: $message")
          schedule.cancel()
          resettingTimeout(context.scheduleOnce(1.second, context.self, "timeout"))
      }
    }
  }

  def demoResettingTimeoutActor(): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val actor = context.spawn(ResettingTimeoutActor(), "resettingTimeoutActor")

      actor ! "start timer"
      Thread.sleep(500)
      actor ! "reset timer"
      Thread.sleep(700)
      actor ! "are u there?"
      Thread.sleep(1200)
      actor ! "not visible"

      Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "demoResettingActor")

    import system.executionContext
    system.scheduler.scheduleOnce(5.seconds, () => system.terminate())
  }

  def main(args: Array[String]): Unit = {
    demoResettingTimeoutActor()
  }
}
