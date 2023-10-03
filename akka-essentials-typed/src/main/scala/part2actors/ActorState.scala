package part2actors

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object ActorState {
  object WordCounterActor {
    def apply(): Behavior[String] = Behaviors.setup { context =>
      var total = 0

      Behaviors.receiveMessage { message =>
        val newCount = message.split(" ").length
        total += newCount
        context.log.info(s"Message word count: $newCount - total count: $total")
        Behaviors.same
      }
    }
  }

  trait SimpleThing

  case object EatChocolate    extends SimpleThing
  case object CleanUpTheFloor extends SimpleThing
  case object LearAkka        extends SimpleThing

  object SimpleHuman {
    def apply(): Behavior[SimpleThing] = Behaviors.setup { context =>
      var happiness = 0

      Behaviors.receiveMessage {
        case EatChocolate =>
          context.log.info(s"[$happiness] Eating chocolate")
          happiness += 1
          Behaviors.same
        case CleanUpTheFloor =>
          context.log.info(s"[$happiness] Cleaning the floor")
          happiness -= 2
          Behaviors.same
        case LearAkka =>
          context.log.info(s"[$happiness] Learning Akka")
          happiness += 99
          Behaviors.same
      }
    }
  }

  object SimpleHuman_V2 {
    def apply(): Behavior[SimpleThing] = statelessHuman(0)

    def statelessHuman(happiness: Int): Behavior[SimpleThing] = Behaviors.receive { (context, message) =>
      message match {
        case CleanUpTheFloor =>
          context.log.info(s"[$happiness] Cleaning the floor")
          statelessHuman(happiness - 2)
        case EatChocolate =>
          context.log.info(s"[$happiness] Eating chocolate")
          statelessHuman(happiness + 1)
        case LearAkka =>
          context.log.info(s"[$happiness] Learning Akka")
          statelessHuman(happiness + 99)
      }
    }
  }

  object WordCounter_V2 {
    def apply(): Behavior[String] = statelessWordCounter(0)

    def statelessWordCounter(total: Int): Behavior[String] = Behaviors.receive { (context, message) =>
      val newCount     = message.split(" ").length
      val updatedTotal = total + newCount
      context.log.info(s"Message word count: $newCount - total count: $updatedTotal")
      statelessWordCounter(updatedTotal)
    }
  }

  def demoSimpleHuman(): Unit = {
    val human = ActorSystem(SimpleHuman_V2(), "demoSimpleHuman")

    human ! LearAkka
    human ! EatChocolate
    (1 to 30).foreach(_ => human ! CleanUpTheFloor)

    Thread.sleep(1000)

    human.terminate()
  }

  def main(args: Array[String]): Unit = {
    demoSimpleHuman()
  }
}
