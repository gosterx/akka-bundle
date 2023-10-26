package part3storesandserialization

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object Cassandra extends App {

  val cassandraActorSystem = ActorSystem("CassandraSystem", ConfigFactory.load().getConfig("cassandraDemo"))
  val persistentActor      = cassandraActorSystem.actorOf(Props[SimplePersistentActor], "simplePersistentActor")

  for (i <- 1 to 10) {
    persistentActor ! s"I love Akka [$i]"
  }
  persistentActor ! "print"
  persistentActor ! "snap"
  for (i <- 11 to 20) {
    persistentActor ! s"I love Akka [$i]"
  }
}
