package com.g5deathmarch.dndbot.groupme

import cats.effect.Concurrent
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

case class GroupMeTextMessage(
  bot_id: String,
  text: String
)

trait GroupMeClient[F[_]] {

  def sendTextGroupMeMessage(message: String): F[Status]

}

class GroupMeClientImpl[F[_]: Concurrent](
  config: GroupMeConfig,
  client: Client[F]
) extends GroupMeClient[F] with StrictLogging {
  val dsl = new Http4sClientDsl[F] {}

  override def sendTextGroupMeMessage(message: String): F[Status] = {
    logger.debug("Attempting to send message to GroupMe Chat")
    val uri = Uri.unsafeFromString(config.postUrl)
    val req = Request[F](Method.POST, uri)
      .withEntity(GroupMeTextMessage(config.botId, message))

    client.status(req)
  }
}

class LocalGroupMeClient[F[_]: Concurrent]() extends GroupMeClient[F] with StrictLogging {

  override def sendTextGroupMeMessage(message: String): F[Status] = {
    logger.debug(s"Text message to send to GroupMe\n$message")
    Concurrent[F].pure(Status.Ok)
  }

}
