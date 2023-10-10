package part3testing

import akka.actor.testkit.typed.CapturedLogEvent
import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, ScalaTestWithActorTestKit, TestInbox}
import org.scalatest.wordspec.AnyWordSpecLike
import org.slf4j.event.Level
import part2actors.ChildActorsExercise._

class SynchronousTestingSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "A word counter master" should {
    "spawn a child upon reception of the initialize message" in {
      val master = BehaviorTestKit(WordCounterMaster())
      master.run(Initialize(1)) // synchronous sending

      // check that effect was produced
      val effect = master.expectEffectType[Spawned[WorkerProtocol]]

      effect.childName shouldEqual "worker1"
    }

    "send a task to a child actor" in {
      val master = BehaviorTestKit(WordCounterMaster())
      master.run(Initialize(1))

      val effect = master.expectEffectType[Spawned[WorkerProtocol]]

      val mailbox = TestInbox[UserProtocol]() // the "requester"
      // start processing
      master.run(WordCountTask("Akka testing is pretty powerful", mailbox.ref))
      // mock the reply
      master.run(WordCountReply(0, 5))
      // test that the request got the right message
      mailbox.expectMessage(Reply(5))
    }

    "log messages" in {
      val master = BehaviorTestKit(WordCounterMaster())
      master.run(Initialize(1))

      master.logEntries() shouldBe Seq(CapturedLogEvent(Level.INFO, "[master] Initializing 1 workers"))
    }
  }
}
