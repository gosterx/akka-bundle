package part4infra

import akka.actor.typed.{ActorSystem, MailboxSelector}
import akka.actor.typed.scaladsl.Behaviors
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}
import utils._

import scala.concurrent.duration.DurationInt

object MailboxesDemo {

  trait Command
  case class SupportTicket(contents: String) extends Command
  case class Log(contents: String)           extends Command

  class SupportTicketPriorityMailbox(settings: akka.actor.ActorSystem.Settings, config: Config)
      extends UnboundedPriorityMailbox(
        PriorityGenerator {
          case SupportTicket(contents) if contents.startsWith("[P0]") => 0
          case SupportTicket(contents) if contents.startsWith("[P1]") => 1
          case SupportTicket(contents) if contents.startsWith("[P2]") => 2
          case SupportTicket(contents) if contents.startsWith("[P3]") => 3
          case _                                                      => 4
        }
      )

  def demoSupportTicketMailbox(): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val actor =
        context.spawn(LoggerActor[Command](), "ticketLogger", MailboxSelector.fromConfig("support-ticket-mailbox"))

      actor ! Log("This is a log that is received first but processed last")
      actor ! SupportTicket("[P1] this thing is broken")
      actor ! SupportTicket("[P0] FIX THIS NOW!")
      actor ! SupportTicket("[P3] Something nice to have")

      Behaviors.empty
    }

    ActorSystem(userGuardian, "demoMailbox", ConfigFactory.load().getConfig("mailboxes-demo"))
      .withFiniteLifespan(5.seconds)
  }

  case object ManagementTicket extends ControlMessage with Command

  def demoControlAwareMailbox(): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val actor =
        context.spawn(LoggerActor[Command](), "controlAwareLogger", MailboxSelector.fromConfig("control-mailbox"))

      actor ! SupportTicket("[P1] this thing is broken")
      actor ! SupportTicket("[P0] FIX THIS NOW!")
      actor ! ManagementTicket

      Behaviors.empty
    }

    ActorSystem(userGuardian, "demoControlAwareMailbox", ConfigFactory.load().getConfig("mailboxes-demo"))
      .withFiniteLifespan(5.seconds)
  }

  def main(args: Array[String]): Unit = {
    demoControlAwareMailbox()
  }
}
