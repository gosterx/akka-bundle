package part3testing

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt
import scala.util.Random

class TimedAssertionsSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  import TimedAssertionsSpec._

  "A worker actor" should {
    val worker = testKit.spawn(WorkerActor(), "worker")
    val probe  = testKit.createTestProbe[ResultMessage]()

    "reply with a work result it at most 1 second" in {
      worker ! Work(probe.ref)
      probe.expectMessage(1.second, WorkResult(42))
    }

    "reply with a work result in at least 500 milliseconds" in {
      worker ! Work(probe.ref)
      probe.expectNoMessage(500.millis)
      probe.expectMessage(500.millis, WorkResult(42))
    }

    "reply with the meaning pf life in between 0.5 and 1 second" in {
      worker ! Work(probe.ref)

      probe.within(500.millis, 1.second) {
        // scenario, run as manu assertion
        probe.expectMessage(WorkResult(42))
      }
    }

    "reply with multiple results in a timely manner" in {
      worker ! WorkSequence(probe.ref)
      val result = probe.receiveMessages(10, 1.second)

      result.collect {
        case WorkResult(value) => value
      }.sum should be > 5
    }
  }
}

object TimedAssertionsSpec {
  trait ResultMessage
  case class WorkResult(result: Int) extends ResultMessage

  trait Message
  case class Work(replyTo: ActorRef[ResultMessage])         extends Message
  case class WorkSequence(replyTo: ActorRef[ResultMessage]) extends Message

  object WorkerActor {
    def apply(): Behavior[Message] = Behaviors.receiveMessage {
      case Work(replyTo) =>
        // wait a bit
        Thread.sleep(500)
        replyTo ! WorkResult(42)
        Behaviors.same
      case WorkSequence(replyTo) =>
        val random = new Random()
        (1 to 10).foreach { _ =>
          Thread.sleep(random.nextInt(50))
          replyTo ! WorkResult(1)
        }
        Behaviors.same
    }
  }
}
