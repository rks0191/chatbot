package bot.handler.memory.storage

import bot.connections.Attribute
import bot.learn.{PossibleReply, SearchResponses}
import bot.handler.memory.Trie
import bot.handler.memory.definition.{Definition, PartOfSentence}

import scala.annotation.tailrec

trait BotStorage[T] {
  def search(storage: T, message: String): SearchResponses
  def add(storage: T, message: List[PartOfSentence], replies: PossibleReply, dictionary: Set[Definition]): T
}

object BotStorage {
  implicit def trieLookup: BotStorage[Trie] = new BotStorage[Trie] {
    /**
      * The algorithm describes the search of a message in a trie, by parsing every word and matching it,
      * thus returning a Set of possible replies depending on bot's previous replies.
      *
      * @param message - the sentence that is to be found, or not
      * @return - returns a Set of (previousMessageFromBot, Set[functions returning possible replies]),
      *         from which another algorithm will pick the best choice.
      */
    override def search(storage: Trie, message: String): SearchResponses = {
      @tailrec
      def go(message:    List[String],
             trie:       Trie,
             attributes: Map[Attribute, String]): SearchResponses = {
        if (message.isEmpty)
          SearchResponses(attributes, trie.replies) //completely ran over all the words
        else {
          val head = message.head

          trie.children.find(t => t.information.wordMatches(head)) match {
            case None           => SearchResponses(attributes) //word wasn't found in the trie
            case Some(nextNode) => go(message.tail, nextNode, nextNode.information.addToAttributes(head, attributes))
          }
        }
      }

      go(toPartsOfSentence(message), storage, Map[Attribute, String]().empty)
    }

    /**
      * For a current Trie, the algorithm returns another trie with the message added.
      * The pattern matching does the following:
      *   1. in case of None => current word isn't in the set => add it, and call the function with the same node
      *   2. in case of Some(next) => next node has been found => remove it from the Set, since its gonna be different 
      * it would double stack otherwise
      */
    override final def add(storage: Trie,
                           message:    List[PartOfSentence],
                           replies:    PossibleReply,
                           dictionary: Set[Definition]): Trie = {
      def go(curr: Trie, words: List[PartOfSentence]): Trie = {
        def ifEmpty(currWord: PartOfSentence): Trie = {
          val newTrie = go(Trie(currWord, message, dictionary), words.tail)
          curr.copy(children = curr.children + newTrie)
        }

        def addToExisting(trie: Trie, rest: List[PartOfSentence]): Trie = {
          val updatedTrie = go(trie, words.tail)
          curr.copy(children = curr.children - trie + updatedTrie)
        }

        if (words.isEmpty)
          this.addReplies(curr, replies)
        else {
          val currWord = words.head
          val next     = curr.children.find(t => t.information.informationMatches(currWord))

          next.fold(ifEmpty(currWord))(t => addToExisting(t, words.tail))
        }
      }

      go(storage, message)
    }

    /**
      * As part of the "storing" journey, this is at the very end - where the replies have to be added.
      *
      * @param replies - replies that are to be added
      * @return     - new leafs which also contain the new replies
      *             There are 2 cases:
      *             1. when the replies depend on a previous bot message ( or lack of ) also stored:
      *             the replies are appended to the already existing replies.
      *             2. when they aren't stored at all:
      *             they are registered as new replies with their attribute.
      */
    private def addReplies(trie: Trie, replies: PossibleReply): Trie =
      trie.replies
        .find(_.previousBotMessage == replies.previousBotMessage)
        .fold(trie.copy(replies = trie.replies + replies))(rep => updateReplies(trie, rep, replies))

    private def updateReplies(trie:       Trie,
                              to:         PossibleReply,
                              newReplies: PossibleReply): Trie =
      trie.copy(replies = trie.replies - to + to.copy(possibleReply = to.possibleReply ++ newReplies.possibleReply))

    private def toPartsOfSentence(msg: String): List[String] =
      msg.split(' ')
        .toList
        .filter(!_.isEmpty)  }
}
