package com.g5deathmarch.dndbot

import com.g5deathmarch.dndbot.groupme.{
  GroupMeConfig,
  GroupMeClientImpl,
  LocalGroupMeClient,
  GroupMeClient
}

import cats.effect._
import cats.syntax.all._
import cats.implicits._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import org.http4s._
import cats.syntax.group
import com.typesafe.scalalogging.StrictLogging
import com.g5deathmarch.dndbot.github.{GithubConfig, GithubClient, GithubClientImpl}
import com.g5deathmarch.dndbot.groupme.GroupMeClient

object Server extends StrictLogging {

  def stream[F[_]: Async]: Stream[F, Nothing] = {
    for {
      client <- Stream.resource(EmberClientBuilder.default[F].build)
      serverConfig: ServerConfig = ServerConfig.load
      groupMeConfig: GroupMeConfig = GroupMeConfig.load
      githubConfig: GithubConfig = GithubConfig.load
      groupMeClient = {
        if (serverConfig.useLocal) {
          new LocalGroupMeClient[F]
        } else
          new GroupMeClientImpl[F](groupMeConfig, client)
      }
      githubClient = {
        new GithubClientImpl[F](githubConfig, client)
      }
      service = new BotService[F](groupMeConfig, groupMeClient, githubClient)
      logHeaders = true
      logBody = true
      httpApp = Logger.httpApp(logHeaders, logBody)(service.routes.orNotFound)

      exitCode <- Stream.resource(
        EmberServerBuilder
          .default[F]
          .withHost(Host.fromString(serverConfig.host).get)
          .withPort(Port.fromInt(serverConfig.port).get)
          .withHttpApp(httpApp)
          .build
          .flatMap(_ => Resource.eval(Async[F].never))
      )
    } yield exitCode
  }

}
