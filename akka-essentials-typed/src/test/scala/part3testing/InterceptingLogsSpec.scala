package part3testing

import akka.actor.testkit.typed.scaladsl.{LoggingTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike

class InterceptingLogsSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  import InterceptingLogsSpec._

  val item              = "AkkaCourse"
  val creditCard        = "1234 1234 1234 1234"
  val invalidCreditCard = "0000 0000 0000 0000"

  "A checkout flow" should {
    "correctly dispatch an order for a valid card" in {
      LoggingTestKit
        .info("Order") // filtering log messages of level INFO which contain the string "Order"
        .withMessageRegex(s"Order [0-9]+ for item $item has been dispatched")
        .withOccurrences(1)
        .expect {
          val checkoutActor = testKit.spawn(Checkout())
          checkoutActor ! Checkout(item, creditCard)
        }
    }

    "freak out if the payment is declined" in {
      LoggingTestKit
        .error[RuntimeException]
        .withOccurrences(1)
        .expect {
          val checkoutActor = testKit.spawn(Checkout())
          checkoutActor ! Checkout(item, invalidCreditCard)
        }
    }
  }
}

object InterceptingLogsSpec {

  trait PaymentProtocol
  case class Checkout(item: String, creditCardDetails: String)                     extends PaymentProtocol
  case class AuthorizeCard(creditCard: String, replyTo: ActorRef[PaymentProtocol]) extends PaymentProtocol
  case object PaymentAccepted                                                      extends PaymentProtocol
  case object PaymentDenied                                                        extends PaymentProtocol
  case class DispatchOrder(item: String, replyTo: ActorRef[PaymentProtocol])       extends PaymentProtocol
  case object OrderConfirmed                                                       extends PaymentProtocol

  object Checkout {
    def apply(): Behavior[PaymentProtocol] = Behaviors.setup { context =>
      val paymentManager    = context.spawn(PaymentManager(), "paymentManager")
      val fulfilmentManager = context.spawn(FulfilmentManager(), "fulfilmentManager")

      def awaitingCheckout(): Behavior[PaymentProtocol] = Behaviors.receiveMessage {
        case Checkout(item, creditCardDetails) =>
          context.log.info(s"Received order for item $item")
          paymentManager ! AuthorizeCard(creditCardDetails, context.self)
          pending(item)
        case _ => Behaviors.same
      }

      def pending(item: String): Behavior[PaymentProtocol] = Behaviors.receiveMessage {
        case PaymentAccepted =>
          fulfilmentManager ! DispatchOrder(item, context.self)
          pendingDispatch(item)
        case PaymentDenied =>
          throw new RuntimeException("Cannot handle invalid payments")
      }

      def pendingDispatch(item: String): Behavior[PaymentProtocol] = Behaviors.receiveMessage {
        case OrderConfirmed =>
          context.log.info(s"Dispatch for $item confirmed")
          awaitingCheckout()
      }

      awaitingCheckout()
    }
  }

  object PaymentManager {
    def apply(): Behavior[PaymentProtocol] = Behaviors.receiveMessage {
      case AuthorizeCard(card, replyTo) =>
        if (card.startsWith("0")) {
          replyTo ! PaymentDenied
        } else {
          replyTo ! PaymentAccepted
        }
        Behaviors.same
      case _ =>
        Behaviors.same
    }
  }

  object FulfilmentManager {
    def apply(): Behavior[PaymentProtocol] = active(43)

    def active(orderId: Int): Behavior[PaymentProtocol] = Behaviors.receive { (context, message) =>
      message match {
        case DispatchOrder(item, replyTo) =>
          context.log.info(s"Order $orderId for item $item has been dispatched")
          replyTo ! OrderConfirmed
          active(orderId + 1)
        case _ =>
          Behaviors.same
      }
    }
  }

}
