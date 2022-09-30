package com.g5deathmarch.dndbot

import com.g5deathmarch.dndbot.groupme.{GroupMeConfig, GroupMeClient, GroupMeService}

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

object Server {

  def stream[F[_]: Async]: Stream[F, Nothing] = {
    for {
      client <- Stream.resource(EmberClientBuilder.default[F].build)
      serverConfig: ServerConfig = ServerConfig.load
      groupMeConfig: GroupMeConfig = GroupMeConfig.load
      groupMeClient = new GroupMeClient[F](groupMeConfig, client)
      groupMeService = new GroupMeService[F](groupMeConfig, groupMeClient)
      httpApp = Logger.httpApp(true, true)(groupMeService.routes.orNotFound)

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
