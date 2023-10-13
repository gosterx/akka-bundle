package part4infra

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import utils._

import scala.concurrent.duration.DurationInt

object RoutersDemo {

  def demoPoolRouter(): Unit = {
    val workerBehavior = LoggerActor[String]()
    val poolRouter     = Routers.pool(5)(workerBehavior).withBroadcastPredicate(_.length > 11) // round robin

    val userGuardian = Behaviors.setup[Unit] { context =>
      val poolActor = context.spawn(poolRouter, "master")

      (1 to 10).foreach { i =>
        poolActor ! s"work task $i"
      }

      Behaviors.empty
    }

    ActorSystem(userGuardian, "demoPool").withFiniteLifespan(2.seconds)
  }

  def demoGroupRouter(): Unit = {
    val serviceKey: ServiceKey[String] = ServiceKey[String]("logWorker")

    val userGuardian = Behaviors.setup[Unit] { context =>
      // in real life workers may be created elsewhere in your code
      val workers = (1 to 5).map(i => context.spawn(LoggerActor[String](), s"worker$i"))
      // register the workers with the service key
      workers.foreach(worker => context.system.receptionist ! Receptionist.Register(serviceKey, worker))

      val groupBehavior: Behavior[String] = Routers.group(serviceKey).withRoundRobinRouting() // random by default
      val groupRouter: ActorRef[String]   = context.spawn(groupBehavior, "workerGroup")

      (1 to 10).foreach { i =>
        groupRouter ! s"work task $i"
      }

      Thread.sleep(1000)
      // add new worker later
      val extraWorker = context.spawn(LoggerActor[String](), "extraWorker")
      context.system.receptionist ! Receptionist.Register(serviceKey, extraWorker)
      (1 to 10).foreach { i =>
        groupRouter ! s"work task $i"
      }

      Behaviors.empty
    }

    ActorSystem(userGuardian, "DemoGroup").withFiniteLifespan(2.seconds)

  }

  def main(args: Array[String]): Unit = {
    demoGroupRouter()
  }
}
