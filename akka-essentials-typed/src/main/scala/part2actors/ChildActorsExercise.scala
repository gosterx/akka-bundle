package part2actors

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object ChildActorsExercise {

  /**
    * Exercise: distributed word counting requester ----- (computational task) ----> WCM ------ (computational task)
    * ----> one child of type WCW requester <---- (computational res) <---- WCM ------ (computational res) <----
    *
    * Scheme for scheduling tasks to children: round robin [1-10] task 1 - child 1 task 2 - child 2 . . . task 10 -
    * child 10 task 11 - child 1 task 12 - child 2 . .
    */

  trait MasterProtocol // messages supported by the master
  trait WorkerProtocol
  trait UserProtocol

  // master messages
  case class Initialize(nChildren: Int)                                   extends MasterProtocol
  case class WordCountTask(text: String, replyTo: ActorRef[UserProtocol]) extends MasterProtocol
  case class WordCountReply(id: Int, count: Int)                          extends MasterProtocol

  // worker messages
  case class WorkerTask(id: Int, text: String) extends WorkerProtocol

  // requester (user) messages
  case class Reply(count: Int) extends UserProtocol

  object WordCounterMaster {
    def apply(): Behavior[MasterProtocol] = Behaviors.receive { (context, message) =>
      message match {
        case Initialize(nChildren) =>
          context.log.info(s"[master] Initializing $nChildren workers")

          val childRefs = for {
            i <- 1 to nChildren
          } yield context.spawn(WordCountWorker(context.self), s"worker$i")

          active(childRefs, 0, 0, Map.empty)
      }
    }

    def active(
      workers: IndexedSeq[ActorRef[WorkerProtocol]],
      currentWorkerIndex: Int,
      currentTaskId: Int,
      repliers: Map[Int, ActorRef[UserProtocol]]
    ): Behavior[MasterProtocol] =
      Behaviors.receive { (context, message) =>
        message match {
          case WordCountReply(id, count) =>
            context.log.info(s"[master] Received answer from $id worker - total count of words is $count")
            repliers(id) ! Reply(count)
            active(workers, currentWorkerIndex, currentTaskId, repliers - id)
          case WordCountTask(text, replyTo) =>
            context.log.info(s"[master] Assigning message '$text' for $currentWorkerIndex worker")
            workers.apply(currentWorkerIndex) ! WorkerTask(currentTaskId, text)
            active(
              workers,
              (currentWorkerIndex + 1) % workers.size,
              currentTaskId + 1,
              repliers + (currentTaskId -> replyTo)
            )
        }
      }
  }

  object WordCountWorker {
    def apply(masterRef: ActorRef[MasterProtocol]): Behavior[WorkerProtocol] = Behaviors.receive { (context, message) =>
      message match {
        case WorkerTask(id, text) =>
          val countOfWords = text.split(" ").length
          context.log.info(s"[${context.self.path.name}] Message '$text' contains $countOfWords words")
          masterRef ! WordCountReply(id, countOfWords)
          Behaviors.same
      }
    }
  }

  object Aggregator {
    def apply(): Behavior[UserProtocol] = active()

    def active(total: Int = 0): Behavior[UserProtocol] = Behaviors.receive { (context, message) =>
      message match {
        case Reply(count) =>
          val updatedTotal = total + count
          context.log.info(s"[aggregator] Previous total - $total, updated total - $updatedTotal")
          active(updatedTotal)
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val userGuardianBehaviour: Behavior[NotUsed] = Behaviors.setup { context =>
      val master     = context.spawn(WordCounterMaster(), "master")
      val aggregator = context.spawn(Aggregator(), "aggregator")

      master ! Initialize(3)
      master ! WordCountTask("hello how are you", aggregator)
      master ! WordCountTask("i am fine", aggregator)
      master ! WordCountTask("and you", aggregator)
      master ! WordCountTask("me too", aggregator)

      Behaviors.empty
    }

    val system = ActorSystem(userGuardianBehaviour, "system")

    Thread.sleep(2000)

    system.terminate()
  }
}
