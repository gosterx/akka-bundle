package part2actors

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors

object ChildActors {

  object Parent {
    trait Command
    case class CreateChild(name: String)  extends Command
    case class TellChild(message: String) extends Command
    case object StopChild                 extends Command
    case object WatchChild                extends Command

    def apply(): Behavior[Command] = idle()

    def idle(): Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case CreateChild(name) =>
          context.log.info(s"[parent] Creating child with name $name")
          val childRef: ActorRef[String] = context.spawn(Child(), name)
          active(childRef)
      }
    }

    def active(childRef: ActorRef[String]): Behavior[Command] = Behaviors
      .receive[Command] { (context, message) =>
        message match {
          case TellChild(message) =>
            context.log.info(s"[parent] Sending message '$message' to child")
            childRef ! message
            Behaviors.same
          case StopChild =>
            context.log.info(s"[parent] Stopping child")
            context.stop(childRef)
            idle()
          case WatchChild =>
            context.log.info(s"[parent] Watching child")
            context.watch(childRef) // can use any actor ref
            Behaviors.same
          case _ =>
            context.log.info("[parent] command not supported")
            Behaviors.same
        }
      }
      .receiveSignal {
        case (context, Terminated(childRefWhichDied)) =>
          context.log.info(s"[parent] Child ${childRefWhichDied.path} was killed by something...")
          idle()
      }
  }

  object Child {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(s"[child ${context.self.path.name}] Received '$message'")
      Behaviors.same
    }
  }

  def demoParentChild(): Unit = {
    import part2actors.ChildActors.Parent._

    val userGuardianBehaviour: Behavior[Unit] = Behaviors.setup { context =>
      // set ip all the important actors in your application
      val parent = context.spawn(Parent(), "DemoParentChild")
      // set up the initial interaction between the actors
      parent ! CreateChild("child")
      parent ! TellChild("hey kid, you there?")
      parent ! WatchChild
      parent ! StopChild
      parent ! CreateChild("child1")
      parent ! TellChild("hey kid, you there?1")

      // user guardian usually has no behaviour of its own
      Behaviors.empty
    }

    val system = ActorSystem(userGuardianBehaviour, "DemoParentChild")
    Thread.sleep(1000)
    system.terminate()
  }

  object Parent_V2 {
    trait Command
    case class CreateChild(name: String)                extends Command
    case class TellChild(name: String, message: String) extends Command
    case class StopChild(name: String)                  extends Command
    case class WatchChild(name: String)                 extends Command

    def apply(): Behavior[Command] = active(Map.empty[String, ActorRef[String]])

    def active(childs: Map[String, ActorRef[String]]): Behavior[Command] = Behaviors
      .receive[Command] { (context, message) =>
        message match {
          case CreateChild(name) if childs.contains(name) =>
            context.log.info(s"[parent] Actor with '$name' already exists")
            active(childs)
          case CreateChild(name) =>
            context.log.info(s"[parent] Creating '$name' child actor")
            val child = context.spawn(Child(), name)
            active(childs + (name -> child))
          case TellChild(name, message) =>
            val childOption = childs.get(name)
            childOption.fold(context.log.info(s"[parent] Child $name could not be found"))(child => child ! message)
            Behaviors.same
          case StopChild(name) =>
            context.log.info(s"[parent] Attempting to stop actor with '$name' name'")
            val childOption = childs.get(name)
            childOption.fold(context.log.info(s"[parent] Child $name could not be found"))(context.stop)
            active(childs - name)
          case WatchChild(name) =>
            context.log.info(s"[parent] Attempting to watch on $name actor")
            val childOption = childs.get(name)
            childOption.fold(context.log.info(s"[parent] Child $name could not be found"))(context.watch)
            Behaviors.same
        }
      }
      .receiveSignal {
        case (context, Terminated(childRef)) =>
          context.log.info(s"[parent] Actor ${childRef.path} was killed")
          Behaviors.same
      }
  }

  def demoParentChildV2(): Unit = {
    import Parent_V2._

    val userGuardianBehaviour: Behavior[NotUsed] = Behaviors.setup { context =>
      val parent = context.spawn(Parent_V2(), "parent")

      parent ! TellChild("child1", "hey 1")
      parent ! CreateChild("child1")
      parent ! CreateChild("child1")
      parent ! CreateChild("child2")
      parent ! TellChild("child2", "hey 2")

      Behaviors.empty
    }

    val system = ActorSystem(userGuardianBehaviour, "demoParentChildV2")
    Thread.sleep(1000)
    system.terminate()
  }

  def main(args: Array[String]): Unit = {
    demoParentChild()
  }
}
