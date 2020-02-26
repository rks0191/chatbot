package bot.handler.memory.part.of.speech

sealed trait GrammaticalNumber

case object Singular extends GrammaticalNumber
case object Plural   extends GrammaticalNumber
