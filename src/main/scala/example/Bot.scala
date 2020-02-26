package example

import bot.connections.{Acquaintances, Attribute}
import bot.handler.{MessageHandler, SessionInformation}
import cats.Monad
import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all._
import example.brain.Manager

import scala.annotation.tailrec
import scala.language.higherKinds

case class Bot[F[_]](minKnowledgeThreshold: Int) extends Manager with MessageHandler {
  type Matcher = (Option[Map[Attribute, String]], SessionInformation)

  implicit def acquaintances(implicit M: Monad[F]): Acquaintances[F] = Acquaintances.xmlStorage[F]("out.xml")

  def startDemo(implicit M: Monad[F]): Unit = {
    def go(sessionInformation: SessionInformation): Unit = {
      val message = scala.io.StdIn.readLine()

      val sessionInformationWithHumanMessage = sessionInformation.addHumanMessage(message)

      message match {
        case "QUIT"                => acquaintances.add(currentSessionInformation.toMap)
        case "Do you remember me?" =>
          val possibleMatches = acquaintances.tryMatch(currentSessionInformation.toList, minKnowledgeThreshold)

          possibleMatches
            .map(pm => matcher(pm, sessionInformationWithHumanMessage))
            .map {
              case (None, si)    => go(si)
              case (Some(p), si) =>
                currentSessionInformation = currentSessionInformation.empty ++ p
                acquaintances.forget(p).map(_ => go(si))
            }
        case _                      =>
          val updatedSessionInformation = sessionInformation.addBotMessage(handle(masterBrain, message, sessionInformationWithHumanMessage))
          updatedSessionInformation.lastBotMessage.foreach(cl => println(cl.message))

          go(updatedSessionInformation)
      }
    }

    go(sessionInformation)
  }

  //TODO this is rather tricky - have maybe a separate brain module which deals with remembering people ...? More generic this way
  @tailrec
  final def matcher(people:   List[Map[Attribute, String]],
                    sessionInformation: SessionInformation): Matcher = {
    if (people.isEmpty) {
      val response = "Sorry, I do not seem to remember you."
      println(response)
      (None, sessionInformation.addBotMessage(response))
    }
    else {
      val botMsg = "Does this represent you: " + people.head
        .filterNot(currentSessionInformation.toList.contains)
        .maxBy(_._1.weight)._2
      println(botMsg)

      val userMsg = scala.io.StdIn.readLine()

      if (userMsg == "Yes") {
        println("Ah, welcome back!")
        (Some(people.head), sessionInformation.addHumanMessage(userMsg).addBotMessage(botMsg))
      }
      else
        matcher(people.tail, sessionInformation.addHumanMessage(userMsg).addBotMessage(botMsg))
    }
  }

  override def sessionInformation: SessionInformation =
    SessionInformation(masterBrain,
      NonEmptyList("Not familiar with this", List.empty),
      Set("", "", "Changed the subject..."))
}
