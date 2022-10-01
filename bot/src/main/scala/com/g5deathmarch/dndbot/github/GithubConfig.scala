package com.g5deathmarch.dndbot.github

import pureconfig._
import pureconfig.generic.auto._

case class GithubConfig(
  username: String,
  accessToken: String,
  repoName: String
)

object GithubConfig {

  def load: GithubConfig = ConfigSource.default.at("dndbot.github").loadOrThrow[GithubConfig]

}
