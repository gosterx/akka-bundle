import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Main {

  object SimpleActor {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      println("123")
      context.log.info(message)
      Behaviors.same
    }
  }

  def main(args: Array[String]): Unit = {
    println("Hello world!")
  }
}
