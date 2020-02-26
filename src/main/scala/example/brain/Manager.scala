package example.brain

import bot.learn.RepliesLearner._
import bot.handler.memory.Trie
import bot.handler.memory.definition.SimpleWord
import example.brain.definitions.Definitions
import example.brain.modules.MasterModule

trait Manager extends MasterModule {
  val masterBrain: Trie = learn(Trie(SimpleWord("".r)), List(jobs, ages, greetings).flatten, Definitions.get())
}
