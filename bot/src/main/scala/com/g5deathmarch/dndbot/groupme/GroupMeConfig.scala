package com.g5deathmarch.dndbot.groupme

import pureconfig._
import pureconfig.generic.auto._

case class GroupMeConfig(
  postUrl: String,
  botId: String
)

object GroupMeConfig {
  def load: GroupMeConfig = ConfigSource.default.at("bot.groupme").loadOrThrow[GroupMeConfig]
}
