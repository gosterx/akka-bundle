package part4practices

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.journal.{Tagged, WriteEventAdapter}
import akka.persistence.query.{Offset, PersistenceQuery}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.DurationInt
import scala.util.Random

object PersistenceQueryDemo extends App {

  val system = ActorSystem("PersistenceQueryDemo", ConfigFactory.load().getConfig("persistenceQuery"))

  // read journal
  val readJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  // give me all persistence ids
  val persistenceIds = readJournal.persistenceIds()

  implicit val meterializer = ActorMaterializer()(system)

  persistenceIds.runForeach { persistenceId =>
    println(s"Found persistence id: $persistenceId")
  }

  class PersistentSimpleActor extends PersistentActor with ActorLogging {
    override def persistenceId: String = "persistence-id-1"

    override def receiveCommand: Receive = {
      case message =>
        persist(message) { _ =>
          log.info(s"Persisted: $message")
        }
    }

    override def receiveRecover: Receive = {
      case event =>
        log.info(s"Recovered: $event")
    }
  }

  val simpleActor = system.actorOf(Props[PersistentSimpleActor], "simpleActor")

//  import system.dispatcher
//  system.scheduler.scheduleOnce(5.seconds) {
//    val message = "ASD"
//    simpleActor ! message
//  }

  // events by persistence id
  val events = readJournal.eventsByPersistenceId("persistence-id-1", 0, Long.MaxValue)
  events.runForeach { event =>
    println(s"Read: $event")
  }

  // events by tag
  val genres = Array("pop", "rock", "hip-hop", "jazz", "disco")
  case class Song(artist: String, title: String, genre: String)
  // command
  case class Playlist(songs: List[Song])
  // event
  case class PlaylistPurchased(id: Int, songs: List[Song])

  class MusicStoreCheckoutActor extends PersistentActor with ActorLogging {

    var latestPlaylistId = 0

    override def persistenceId: String = "music-store-checkout"

    override def receiveCommand: Receive = {
      case Playlist(songs) =>
        persist(PlaylistPurchased(latestPlaylistId, songs)) { e =>
          log.info(s"User purchased $songs")
          latestPlaylistId += 1
        }
    }

    override def receiveRecover: Receive = {
      case event @ PlaylistPurchased(id, _) =>
        log.info(s"Recovered: $event")
        latestPlaylistId = id
    }
  }

  class MusicStoreEventAdapter extends WriteEventAdapter {
    override def manifest(event: Any): String = "musicStore"

    override def toJournal(event: Any): Any = event match {
      case event @ PlaylistPurchased(_, songs) =>
        val genres = songs.map(_.genre).toSet
        Tagged(event, genres)
      case event => event
    }
  }

  val checkoutActor = system.actorOf(Props[MusicStoreCheckoutActor], "musicStoreActor")

  val r = new Random
  for (_ <- 1 to 10) {
    val maxSongs = r.nextInt(5)
    val songs = for (i <- 1 to maxSongs) yield {
      val randomGenre = genres(r.nextInt(5))
      Song(s"Artist $i", s"SONG$i", randomGenre)
    }

    checkoutActor ! Playlist(songs.toList)
  }

  val rockPlaylist = readJournal.eventsByTag("rock", Offset.noOffset)

  rockPlaylist.runForeach { event =>
    println(s"Found playlist with a rock song: $event")
  }
}
