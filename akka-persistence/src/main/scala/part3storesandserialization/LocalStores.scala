package part3storesandserialization

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.typesafe.config.ConfigFactory

object LocalStores extends App {

  class SimplePersistentActor extends PersistentActor with ActorLogging {

    var nMessages = 0

    override def persistenceId: String = "simple-persistent-actor"

    override def receiveCommand: Receive = {
      case "print" =>
        log.info(s"I have persisted $nMessages so far")
      case "snap" =>
        saveSnapshot(nMessages)
      case SaveSnapshotSuccess(metadata) =>
        log.info(s"Save snapshot was successful: $metadata")
      case SaveSnapshotFailure(metadata, cause) =>
        log.warning(s"Save snapshot failed: $cause")
      case message =>
        persist(message) { _ =>
          log.info(s"Persisting $message")
          nMessages += 1
        }
    }

    override def receiveRecover: Receive = {
      case RecoveryCompleted =>
        log.info("Recovery done")
      case SnapshotOffer(metadata, payload: Int) =>
        log.info(s"Recovered snapshot: $payload")
        nMessages = payload
      case message =>
        log.info(s"Recovered: $message")
        nMessages += 1
    }
  }

  val localStoresActorSystem = ActorSystem("LocalStoresSystem", ConfigFactory.load().getConfig("localStores"))
  val persistentActor        = localStoresActorSystem.actorOf(Props[SimplePersistentActor], "simplePersistentActor")

  for (i <- 1 to 10) {
    persistentActor ! s"I love Akka [$i]"
  }
  persistentActor ! "print"
  persistentActor ! "snap"
  for (i <- 11 to 20) {
    persistentActor ! s"I love Akka [$i]"
  }

}
