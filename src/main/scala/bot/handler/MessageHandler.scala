package bot.handler

import bot.connections.Attribute
import bot.learn.PossibleReply
import bot.handler.memory.Trie
import bot.handler.memory.storage.BotStorage

import scala.collection.mutable
import scala.util.Random

trait MessageHandler {
  def sessionInformation: SessionInformation
  var currentSessionInformation: mutable.Map[Attribute, String] = mutable.Map[Attribute, String]()

  def handle[T](storage:         T,
             msg:                String,
             sessionInformation: SessionInformation)(implicit lookup: BotStorage[T]): String = {
    val response = lookup.search(storage, msg)

    if (response.possibleReplies.isEmpty) {
      sessionInformation.randomUnknownMessageReply
    }
    else {
      currentSessionInformation ++= response.attributesFound

      //just in case it's the first message and there are no previous bot messages
      val lastBotMessage = sessionInformation.lastBotMessage.fold("")(_.message)

      provideResponse(response.possibleReplies, lastBotMessage)
    }
  }

  def isDisapproved(brain: Trie, msg: String): String = {
    ""
  }

  /**
    * First off, all the functions are being applied so that all the replies are known.
    * After that, it is tried to find a match for the last bot message.
    * In that case, it is provided a reply for that specific case.
    * Otherwise, all the possible replies that are dependent on the last message the bot sent are
    * disregarded and all the other are flatMapped
    * and sent as a parameter to a function which will arbitrarily
    * choose a reply.
    *
    * @param possibleReplies = a set of optional functions
    *                        who return a set of string representing the last message the bot sent,
    *                        and a set of functions returning a string representing possible replies.
    * @return a message suitable for the last input the client gave.
    */
  private def provideResponse(possibleReplies: Set[PossibleReply],
                              lastBotMsg:      String): String = {

    val appliedFunctions = possibleReplies map AppliedFunctions.toAppliedFunctions

    lazy val noPreviousBotMessageMatches = appliedFunctions
      .withFilter(_.hasNoPreviousBotMessage)
      .flatMap(_.appliedFunctions)

    appliedFunctions
      .find(_.isAnswerToPreviousBotMessage(lastBotMsg))
      .fold(provideReply(noPreviousBotMessageMatches))(reply => provideReply(reply.appliedFunctions))
  }

  def getAttribute(attribute: Attribute): Option[String] = currentSessionInformation.get(attribute)

  private def provideReply(replies: Set[String]): String =
    if (replies.isEmpty) sessionInformation.randomUnknownMessageReply
    else                 Random.shuffle(replies).head

}
