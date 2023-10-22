package part2eventsourcing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

import java.util.Date

object MultiplePersists extends App {

  /*
   * Diligent accountant: with every invoice, will persist TWO events
   * - a tax record for the financial authority
   * - an invoice record for personal logs or some auditing authority
   * */

  // command
  case class Invoice(recipient: String, date: Date, amount: Int)

  // events
  case class TaxRecord(taxId: String, recordId: Int, date: Date, totalAmount: Int)
  case class InvoiceRecord(invoiceRecordId: Int, recipient: String, date: Date, amount: Int)

  object DiligentAccountant {
    def props(taxId: String, taxAuthority: ActorRef) = Props(new DiligentAccountant(taxId, taxAuthority))
  }

  class DiligentAccountant(taxId: String, taxAuthority: ActorRef) extends PersistentActor with ActorLogging {

    var latestTaxRecordId     = 0
    var latestInvoiceRecordId = 0

    override def persistenceId: String = "diligent-accountant"

    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) =>
        // under the hood works like "journal ! TaxRecord"
        persist(TaxRecord(taxId, latestTaxRecordId, date, amount / 3)) { record =>
          taxAuthority ! record
          latestTaxRecordId += 1

          persist("tax declaration") { declaration =>
            taxAuthority ! declaration
          }
        }
        // under the hood works like "journal ! InvoiceRecord"
        persist(InvoiceRecord(latestInvoiceRecordId, recipient, date, amount)) { invoiceRecord =>
          taxAuthority ! invoiceRecord
          latestInvoiceRecordId += 1

          persist("invoice declaration") { declaration =>
            taxAuthority ! declaration
          }
        }
    }

    override def receiveRecover: Receive = {
      case event => log.info(s"Recovered: $event")
    }
  }

  class TaxAuthority extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"Received: $message")
    }
  }

  val system       = ActorSystem("MultiplePersistsDemo")
  val taxAuthority = system.actorOf(Props[TaxAuthority], "HMRC")
  val accountant   = system.actorOf(DiligentAccountant.props("UK432423_1233312", taxAuthority))

  accountant ! Invoice("SSP", new Date, 2000)
}
