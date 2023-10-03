package part2actors

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object ActorsIntro {

  // behavior
  val simpleActorBehaviour: Behavior[String] = Behaviors.receiveMessage { (message: String) =>
    // do something with the message
    println(s"[simple actor] I have received: $message")

    // new behaviour for the NEXT message
    Behaviors.same
  }

  def demoSimpleActor(): Unit = {
    // instantiate
    val actorSystem = ActorSystem(SimpleActor_V2(), "FirstActorSystem")

    // communicate
    actorSystem ! "I am learning Akka" // asynchronously send a message

    // shut down
    Thread.sleep(1000)
    actorSystem.terminate()
  }

  // "refactor"
  object SimpleActor {
    def apply(): Behavior[String] = Behaviors.receiveMessage { (message: String) =>
      // do something with the message
      println(s"[simple actor] I have received: $message")

      // new behaviour for the NEXT message
      Behaviors.same
    }
  }

  object SimpleActor_V2 {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      // context is a data structure (ActorContext) with access to variety of APIs
      // simple example: logging
      context.log.info(s"[simple actor] I have received: $message")
      Behaviors.same
    }
  }

  object SimpleActor_V3 {
    def apply(): Behavior[String] = Behaviors.setup { context =>
      // actor "private" data and methods, behaviours etc

      // behaviour of the FIRST message
      Behaviors.receiveMessage { message =>
        context.log.info(s"[simple actor] I have received: $message")
        Behaviors.same
      }
    }
  }

  /**
    * Exercises
    *   1. Define two "person" actor behaviors, which receive Strings:
    *      - "happy", which logs your message, e.g. "I've received ____. That's great!"
    *      - "sad", .... "That sucks." Test both.
    *
    * 2. Change the actor behavior:
    *   - the happy behavior will turn to sad() if it receives "Akka is bad."
    *   - the sad behavior will turn to happy() if it receives "Akka is awesome!"
    *
    * 3. Inspect my code and try to make it better.
    */

  object Person {
    def happy(): Behavior[String] = Behaviors.receive { (context, message) =>
      message match {
        case "Akka is bad." =>
          context.log.info(s":(")
          sad()
        case _ =>
          context.log.info(s"I've received '$message'. That's great!")
          Behaviors.same
      }
    }

    def sad(): Behavior[String] = Behaviors.receive { (context, message) =>
      message match {
        case "Akka is awesome!" =>
          context.log.info("Indeed!")
          happy()
        case _ =>
          context.log.info("That sucks.")
          Behaviors.same
      }
    }
  }

  def testPerson(): Unit = {
    val actorSystem = ActorSystem(Person.sad(), "PersonAkkaSystem")

    actorSystem ! "123"
    actorSystem ! "Akka is awesome!"
    actorSystem ! "Akka is awesome!"

    actorSystem.terminate()
  }

  object WierdActor {
    def apply(): Behavior[Any] = Behaviors.receive { (context, message) =>
      message match {
        case number: Int =>
          context.log.info(s"int $number")
          Behaviors.same
        case string: String =>
          context.log.info(s"string $string")
          Behaviors.same
      }
    }
  }

  // solution: add wrapper types and type hierarchy

  object BetterActor {
    trait Message
    case class IntMessage(number: Int)        extends Message
    case class StringMessage(message: String) extends Message

    def apply(): Behavior[Message] = Behaviors.receive { (context, message) =>
      message match {
        case IntMessage(number) =>
          context.log.info(s"int $number")
          Behaviors.same
        case StringMessage(string) =>
          context.log.info(s"string $string")
          Behaviors.same
      }
    }
  }

  def main(args: Array[String]): Unit = {
    testPerson()
  }
}
