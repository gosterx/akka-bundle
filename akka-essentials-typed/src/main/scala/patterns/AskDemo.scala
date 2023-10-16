package patterns

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.util.Timeout
import utils.ActorSystemEnhancements

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Try, Success, Failure}

object AskDemo {

  trait WorkProtocol
  case class ComputationalTask(payload: String, replyTo: ActorRef[WorkProtocol]) extends WorkProtocol
  case class ComputationalResult(result: Int)                                    extends WorkProtocol

  object Worker {
    def apply(): Behavior[WorkProtocol] = Behaviors.receive { (context, message) =>
      message match {
        case ComputationalTask(payload, replyTo) =>
          context.log.info(s"[worker] Crunching data for $payload")
          replyTo ! ComputationalResult(payload.split(" ").length)
          Behaviors.same
        case _ =>
          Behaviors.same
      }
    }
  }

  def askSimple(): Unit = {
    import akka.actor.typed.scaladsl.AskPattern._

    val system                        = ActorSystem(Worker(), "demoAskSimple")
    implicit val timeout: Timeout     = Timeout(3.seconds)
    implicit val scheduler: Scheduler = system.scheduler

    val reply: Future[WorkProtocol] = system.ask(ref => ComputationalTask("asd asd asd", ref))

    implicit val ec: ExecutionContext = system.executionContext

    reply.foreach(println)

    system.withFiniteLifespan(5.seconds)
  }

  def askFromWithinAnotherActor(): Unit = {
    val userGuardian = Behaviors.setup[WorkProtocol] { context =>
      val worker = context.spawn(Worker(), "worker")

      case class ExtendedComputationalResult(count: Int, description: String) extends WorkProtocol

      implicit val timeout: Timeout = Timeout(3.seconds)

      context.ask(worker, ref => ComputationalTask("This Akka pattern", ref)) {
        case Success(ComputationalResult(count)) => ExtendedComputationalResult(count, "This is it")
        case Failure(ex)                         => ExtendedComputationalResult(-1, s"Computation failed $ex")
      }

      Behaviors.receiveMessage {
        case ExtendedComputationalResult(count, description) =>
          context.log.info(s"[UserGuardian] $count - $description")
          Behaviors.same
        case _ =>
          Behaviors.same
      }
    }

    ActorSystem(userGuardian, "demoAskFromWithinAnotherAcotr").withFiniteLifespan(5.seconds)
  }

  def main(args: Array[String]): Unit = {
    askFromWithinAnotherActor()
  }
}
