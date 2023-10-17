package patterns

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import utils.ActorSystemEnhancements

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object PipeDemo {

  val db: Map[String, Int] = Map(
    "Aliaksei" -> 123,
    "Jane"     -> 546
  )

  val executor    = Executors.newFixedThreadPool(4)
  implicit val ec = ExecutionContext.fromExecutor(executor)

  def callExternalService(name: String): Future[Int] =
    Future(db(name))

  trait PhoneCallProtocol
  case class FindAndCallPhoneNumber(name: String)    extends PhoneCallProtocol
  case class InitiatePhoneCallInANumber(number: Int) extends PhoneCallProtocol
  case class LogPhoneCallFailure(cause: Throwable)   extends PhoneCallProtocol

  object PhoneCallActor {
    def apply(): Behavior[PhoneCallProtocol] = Behaviors.receive { (context, message) =>
      message match {
        case FindAndCallPhoneNumber(name) =>
          context.log.info(s"Fetching the phone number for $name")
          // pipe pattern
          val phoneNumberFuture = callExternalService(name)
          context.pipeToSelf(phoneNumberFuture) {
            case Success(value)     => InitiatePhoneCallInANumber(value)
            case Failure(exception) => LogPhoneCallFailure(exception)
          }
          Behaviors.same
        case InitiatePhoneCallInANumber(number) =>
          context.log.info(s"Initiating phone call to $number")
          Behaviors.same
        case LogPhoneCallFailure(cause) =>
          context.log.warn(s"Initiating phone call failed: $cause")
          Behaviors.same
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val actor = context.spawn(PhoneCallActor(), "phoneCallActor")

      actor ! FindAndCallPhoneNumber("Aliaksei")
      actor ! FindAndCallPhoneNumber("Ben")

      Behaviors.empty
    }

    ActorSystem(userGuardian, "DemoPipePattern").withFiniteLifespan(2.seconds)
  }
}
