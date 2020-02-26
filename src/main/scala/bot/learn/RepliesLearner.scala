package bot.learn

import bot.handler.memory.Trie
import bot.handler.memory.definition.{Definition, PartOfSentence}
import bot.handler.memory.storage.BotStorage

object RepliesLearner {
  def learn(trie:       Trie,
            acquired:   List[MessageTemplate],
            dictionary: Set[Definition])(implicit storer: BotStorage[Trie]): Trie = {
    acquired.foldLeft(trie)((t, w) => storer.add(t, toWords(w.humanMessage.message), PossibleReply(w), dictionary))
  }

  /**
    * The anonymous function creates a List of Lists of (Regex, Some(Attribute)),
    * which will be flattened => a list of words. The list is then filtered to not contain any
    * empty words ( "" ).
    *
    * @param message - message to be added in the trie that needs to be split
    * @return a list of words that could be either a string with no attr set,
    *         or a regex with an attribute
    */
  private def toWords(message: List[Message]): List[PartOfSentence] =
    message flatMap { w =>
      w.pattern.toString
        .split(" ")
        .toList
        .withFilter(_ != "")
        .map(p => PartOfSentence(p.r, w.attribute))
    }
}
