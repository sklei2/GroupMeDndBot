package com.g5deathmarch.dndbot.groupme

import pureconfig._
import pureconfig.generic.auto._

case class GroupMeConfig(
  postUrl: String,
  botId: String,
  useLocal: Boolean
)

object GroupMeConfig {
  def load: GroupMeConfig = ConfigSource.default.at("dndbot.groupme").loadOrThrow[GroupMeConfig]
}
