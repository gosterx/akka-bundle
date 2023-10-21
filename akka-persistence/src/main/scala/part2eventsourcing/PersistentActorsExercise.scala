package part2eventsourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

import scala.collection.mutable
import scala.collection.mutable.{Map => MutableMap}

object PersistentActorsExercise extends App {

  /*
   * Persistent actor for a voting station
   * Keep:
   *   - the citizen who voted
   *   - the poll: mapping between a candidate and the number of received votes so far
   * */
  case class Vote(citizenPID: String, candidate: String)
  case object ShowState

  case class VoteRecorded(id: Int, citizenPID: String, candidate: String)

  class VotingStation extends PersistentActor with ActorLogging {

    val citizens: mutable.Set[String]  = new mutable.HashSet[String]()
    val poll: mutable.Map[String, Int] = new mutable.HashMap[String, Int]()

    override def persistenceId: String = "voting-station"

    override def receiveCommand: Receive = {
      case vote @ Vote(citizenPID, candidate) =>
        if (!citizens.contains(vote.citizenPID)) {
          persist(vote) { _ => // command sourcing
            log.info(s"Persisted: $vote")
            handleInternalStateChange(citizenPID, candidate)
          }
        } else {
          log.warning(s"Citizen $citizenPID is trying to vote multiple times!")
        }

      case ShowState =>
        log.info(s"Current state: \ncitizens - $citizens,\n poll - $poll")
    }

    override def receiveRecover: Receive = {
      case vote @ Vote(citizenPID, candidate) =>
        log.info(s"Recovered: $vote")
        handleInternalStateChange(citizenPID, candidate)
    }

    def handleInternalStateChange(citizenPID: String, candidate: String): Unit = {
      citizens.add(citizenPID)
      val candidateVotes = poll.getOrElse(candidate, 0)
      poll.put(candidate, candidateVotes + 1)
    }
  }

  val system        = ActorSystem("VotingStation")
  val votingStation = system.actorOf(Props[VotingStation], "votingStation")

  val votesMap = Map[String, String](
    "Alice" -> "Mike",
    "Bob"   -> "Pod",
    "Bob"   -> "Pod",
    "Jenny" -> "Aliaksei",
    "Ghar"  -> "Aliaksei",
    "Dave"  -> "Gen",
    "Lua"   -> "Pod"
  )

  votesMap.foreach {
    case (citizenPID, candidate) =>
      votingStation ! Vote(citizenPID, candidate)
  }

  votingStation ! ShowState
}
