package com.g5deathmarch.dndbot

import pureconfig._
import pureconfig.generic.auto._

case class ServerConfig(
  host: String,
  port: Int,
  useLocal: Boolean
)

object ServerConfig {

  def load: ServerConfig = ConfigSource.default.at("dndbot.server").loadOrThrow[ServerConfig]

}
