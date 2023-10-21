package part2eventsourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

import java.util.Date

object PersistentActors extends App {
  // command
  case class Invoice(recipient: String, date: Date, amount: Int)
  case class InvoiceBulk(invoices: List[Invoice])

  // special commands
  case object Shutdown

  // event
  case class InvoiceRecorded(id: Int, recipient: String, date: Date, amount: Int)

  class Accountant extends PersistentActor with ActorLogging {

    var latestInvoiceId = 0
    var totalAmount     = 0

    override def persistenceId: String = "simple-accountant"

    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) =>
        /*
         * When you receive a command
         * 1) you create an EVENT to persist into the store
         * 2) you persist the event, then pass in a callback that will be triggered once the event is written
         * 3) we update the actor's state when the event has persisted
         * */
        log.info(s"Receive invoice for amount: $amount")
        val event = InvoiceRecorded(latestInvoiceId, recipient, date, amount)
        persist(event) /* time gap: all other messages sent to this actor are STASHED */ { e =>
          latestInvoiceId += 1
          totalAmount += amount
          log.info(s"Persisted $e as invoice #${e.id}, for total amount $totalAmount")
        }

      case InvoiceBulk(invoices) =>
        /*
         * 1) create events
         * 2) persist all the events
         * 3) update the actor state when each event is persisted
         * */
        val invoiceIds = latestInvoiceId to (latestInvoiceId + invoices.size)
        val events = invoices.zip(invoiceIds).map {
          case (invoice, id) => InvoiceRecorded(id, invoice.recipient, invoice.date, invoice.amount)
        }
        persistAll(events) { e =>
          latestInvoiceId += 1
          totalAmount += e.amount
          log.info(s"Persisted SINGLE $e as invoice #${e.id}, for total amount $totalAmount")
        }

      case Shutdown => context.stop(self)
    }

    override def receiveRecover: Receive = {
      case InvoiceRecorded(id, _, _, amount) =>
        latestInvoiceId = id
        totalAmount += amount
        log.info(s"Recovered invoice #$id for amount $amount, total amount: $totalAmount")
    }

    /*
     * This method is called if persisting failed
     * This actor will be STOPPED
     *
     * Best practice: start the actor again after a while.
     * (use Backoff supervisor)
     * */
    override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Fail to persist $event because of $cause")
      super.onPersistFailure(cause, event, seqNr)
    }

    /*
     * Called if the JOURNAL fails to persist the event
     * The actor is RESUMED
     * */
    override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Persist rejected $event because of $cause")
    }
  }

  val system     = ActorSystem("PersistentActors")
  val accountant = system.actorOf(Props[Accountant], "simpleAccountant")

  for (i <- 1 to 10) {
    accountant ! Invoice("Company", new Date, i * 1000)
  }

  val newInvoices = for (i <- 1 to 5) yield Invoice("SSP", new Date, i * 2000)

  accountant ! InvoiceBulk(newInvoices.toList)

  /*
  * Shutdown of persistent actors
  *
  * Best practice: define your own "shutdown" message
  * */
  accountant ! Shutdown
}
