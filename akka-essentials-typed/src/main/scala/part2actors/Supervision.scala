package part2actors

import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy, Terminated}
import akka.actor.typed.scaladsl.Behaviors

object Supervision {

  object FussyWordCounter {
    def apply(): Behavior[String] = active()

    def active(total: Int = 0): Behavior[String] = Behaviors.receive { (context, message) =>
      val wordCount = message.split(" ").length

      if (message.startsWith("Q")) throw new RuntimeException("I HATE queues!")
      if (message.startsWith("W")) throw new NullPointerException

      context.log.info(s"Received piece of text: '$message', counted $wordCount words, total - ${total + wordCount}")

      active(total + wordCount)
    }
  }

  // actor throwing exception gets killed

  def demoCrash(): Unit = {
    val userGuardian: Behavior[NotUsed] = Behaviors.setup { context =>
      val fussyCounter = context.spawn(FussyWordCounter(), "fussyCounter")

      fussyCounter ! "Starting to do"
      fussyCounter ! "Quick"
      fussyCounter ! "123"

      Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "system")
    Thread.sleep(1000)
    system.terminate()
  }

  def demoWithParent(): Unit = {
    val parentBehavior: Behavior[String] = Behaviors.setup[String] { context =>
      val child = context.spawn(FussyWordCounter(), "fussyChild")
      context.watch(child)

      Behaviors
        .receiveMessage[String] { message =>
          child ! message
          Behaviors.same
        }
        .receiveSignal {
          case (context, Terminated(child)) =>
            context.log.info(s"Child failed: ${child.path.name}")
            Behaviors.same
        }
    }

    val guardian: Behavior[NotUsed] = Behaviors.setup { context =>
      val parent = context.spawn(parentBehavior, "fussyCounter")

      parent ! "Starting to do"
      parent ! "Quick"
      parent ! "123"

      Behaviors.empty
    }

    val system = ActorSystem(guardian, "systemDemoCrashWithParent")
    Thread.sleep(1000)
    system.terminate()
  }

  def demoSupervisionWithRestart(): Unit = {
    val parentBehavior: Behavior[String] = Behaviors.setup[String] { context =>
      // supervise the child with a restart "strategy"

      val childBehavior =
        Behaviors.supervise(FussyWordCounter()).onFailure[RuntimeException](SupervisorStrategy.restart)

      val child = context.spawn(childBehavior, "fussyChild")
      context.watch(child)

      Behaviors
        .receiveMessage[String] { message =>
          child ! message
          Behaviors.same
        }
        .receiveSignal {
          case (context, Terminated(child)) =>
            context.log.info(s"Child failed: ${child.path.name}")
            Behaviors.same
        }
    }

    val guardian: Behavior[NotUsed] = Behaviors.setup { context =>
      val parent = context.spawn(parentBehavior, "fussyCounter")

      parent ! "Starting to do"
      parent ! "Quick"
      parent ! "123"

      Behaviors.empty
    }

    val system = ActorSystem(guardian, "DemoCrashWithSupervision")
    Thread.sleep(1000)
    system.terminate()
  }

  val differentStratagies = Behaviors
    .supervise(
      Behaviors.supervise(FussyWordCounter()).onFailure[NullPointerException](SupervisorStrategy.resume)
    )
    .onFailure[IndexOutOfBoundsException](SupervisorStrategy.restart)

  def main(args: Array[String]): Unit = {
    demoSupervisionWithRestart()
  }
}
