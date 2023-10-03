package part2actors

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.collection.mutable.{Map => MutableMap}

object BreakingActorEncapsulation {

  // naive bank account
  trait AccountCommand
  case class Deposit(cardId: String, amount: Double)  extends AccountCommand
  case class Withdraw(cardId: String, amount: Double) extends AccountCommand
  case class CreateCreditCard(cardId: String)         extends AccountCommand
  case object CheckCardStatuses                       extends AccountCommand

  trait CreditCardCommand
  case class AttachToAccount(
    balances: MutableMap[String, Double],
    cards: MutableMap[String, ActorRef[CreditCardCommand]]
  ) extends CreditCardCommand
  case object CheckStatus extends CreditCardCommand

  object NaiveBankAccount {
    def apply(): Behavior[AccountCommand] = Behaviors.setup { context =>
      val accountBalances: MutableMap[String, Double]              = MutableMap.empty
      val cardMap: MutableMap[String, ActorRef[CreditCardCommand]] = MutableMap.empty

      Behaviors.receiveMessage {
        case CreateCreditCard(cardId) =>
          context.log.info(s"Creating card $cardId")
          // create credit card child
          val createdCardRef = context.spawn(CreditCard(cardId), cardId)
          // give refferal bonus
          accountBalances += cardId -> 10
          // send an AttachToAccount to the child
          createdCardRef ! AttachToAccount(accountBalances, cardMap)
          // change behaviour
          Behaviors.same
        case Deposit(cardId, amount) =>
          val oldBalance: Double = accountBalances.getOrElse(cardId, 0)
          context.log.info(s"Depositing $amount via $cardId, balance on card: ${oldBalance + amount}")
          accountBalances += cardId -> (oldBalance + amount)
          Behaviors.same
        case Withdraw(cardId, amount) =>
          val oldBalance: Double = accountBalances.getOrElse(cardId, 0)
          if (oldBalance < amount) {
            context.log.warn(s"Attempted withdraw $amount via $cardId: insufficient funds")
          } else {
            accountBalances += cardId -> (oldBalance - amount)
            context.log.info(s"Withdrawing $amount via $cardId,, balance on card: ${oldBalance - amount}")
          }
          Behaviors.same
        case CheckCardStatuses =>
          context.log.info("Checking all card statuses")
          cardMap.values.foreach(creditCard => creditCard ! CheckStatus)
          Behaviors.same
      }
    }
  }

  object CreditCard {
    def apply(cardId: String): Behavior[CreditCardCommand] = Behaviors.receive { (context, message) =>
      message match {
        case AttachToAccount(balances, cards) =>
          context.log.info(s"[$cardId] Attaching to bank account")
          balances += cardId -> 0
          cards += cardId    -> context.self
          Behaviors.same
        case CheckStatus =>
          context.log.info(s"[$cardId] All things green")
          Behaviors.same
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val userGuardian: Behavior[NotUsed] = Behaviors.setup { context =>
      val bankAccount = context.spawn(NaiveBankAccount(), "account")

      bankAccount ! CreateCreditCard("gold")
      bankAccount ! CreateCreditCard("premium")

      bankAccount ! Deposit("gold", 1000)
      bankAccount ! CheckCardStatuses

      Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "system")

    Thread.sleep(1000)
    system.terminate()
  }
}
