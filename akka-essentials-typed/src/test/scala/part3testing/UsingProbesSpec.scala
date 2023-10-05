package part3testing

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike

class UsingProbesSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  import part3testing.UsingProbesSpec._

  "A master actor" should {
    "register a worker" in {
      val master        = testKit.spawn(Master(), "master")
      val workerProbe   = testKit.createTestProbe[WorkerTask]()
      val externalProbe = testKit.createTestProbe[ExternalProtocol]()

      master ! Register(workerProbe.ref, externalProbe.ref)
      externalProbe.expectMessage(RegisterAck)
    }

    "send a task to the worker actor" in {
      val master        = testKit.spawn(Master(), "master")
      val workerProbe   = testKit.createTestProbe[WorkerTask]()
      val externalProbe = testKit.createTestProbe[ExternalProtocol]()

      val testString = "I love Akka"

      master ! Register(workerProbe.ref, externalProbe.ref)
      externalProbe.expectMessage(RegisterAck)
      master ! Work(testString, externalProbe.ref)

      workerProbe.expectMessage(WorkerTask(testString, master.ref, externalProbe.ref))
      // mocking the interaction with Worker actor
      master ! WorkCompleted(3, externalProbe.ref)
      externalProbe.expectMessage(Report(3))
    }

    "aggregate data correctly" in {
      val master        = testKit.spawn(Master(), "master")
      val externalProbe = testKit.createTestProbe[ExternalProtocol]()

      val mockedWorkerBehavior = Behaviors.receiveMessage[WorkerTask] {
        case WorkerTask(_, master, originalDestination) =>
          master ! WorkCompleted(3, originalDestination)
          Behaviors.same
      }
      val workerProbe  = testKit.createTestProbe[WorkerTask]()
      val mockedWorker = testKit.spawn(Behaviors.monitor(workerProbe.ref, mockedWorkerBehavior))

      val testString = "I love Akka"

      master ! Register(mockedWorker.ref, externalProbe.ref)
      externalProbe.expectMessage(RegisterAck)

      master ! Work(testString, externalProbe.ref)
      master ! Work(testString, externalProbe.ref)

      externalProbe.expectMessage(Report(3))
      externalProbe.expectMessage(Report(6))
    }
  }
}

object UsingProbesSpec {

  trait MasterProtocol
  case class Work(text: String, replyTo: ActorRef[ExternalProtocol])                        extends MasterProtocol
  case class WorkCompleted(count: Int, originalDestination: ActorRef[ExternalProtocol])     extends MasterProtocol
  case class Register(workerRef: ActorRef[WorkerTask], replyTo: ActorRef[ExternalProtocol]) extends MasterProtocol

  case class WorkerTask(text: String, master: ActorRef[MasterProtocol], originalDestination: ActorRef[ExternalProtocol])

  trait ExternalProtocol
  case class Report(totalCount: Int) extends ExternalProtocol
  case object RegisterAck            extends ExternalProtocol

  object Master {
    def apply(): Behavior[MasterProtocol] = Behaviors.receiveMessage {
      case Register(workerRef, replyTo) =>
        replyTo ! RegisterAck
        active(workerRef)
      case _ =>
        Behaviors.same
    }

    def active(workerRef: ActorRef[WorkerTask], totalCount: Int = 0): Behavior[MasterProtocol] = Behaviors.receive {
      (context, message) =>
        message match {
          case Work(text, replyTo) =>
            workerRef ! WorkerTask(text, context.self, replyTo)
            Behaviors.same
          case WorkCompleted(total, destination) =>
            val newCount = totalCount + total
            destination ! Report(newCount)
            active(workerRef, newCount)
        }
    }
  }

  // Object Worker { ... }
}
