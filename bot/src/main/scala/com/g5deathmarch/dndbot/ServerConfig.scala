package com.g5deathmarch.dndbot

import pureconfig._
import pureconfig.generic.auto._

case class ServerConfig(
  host: String,
  port: Int
)

object ServerConfig {

  def load: ServerConfig = ConfigSource.default.at("bot.server").loadOrThrow[ServerConfig]

}
