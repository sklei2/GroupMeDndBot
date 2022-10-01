package com.g5deathmarch.dndbot.groupme

import cats.effect.Concurrent
import cats.implicits._
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.typelevel.ci.CIString
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}
import io.circe.generic.auto._
import io.circe.{Encoder, Decoder}
import org.http4s.circe.{jsonOf, jsonEncoderOf}
import cats.effect.kernel.Async
import com.typesafe.scalalogging.StrictLogging

case class GroupMeTextMessage(
  bot_id: String,
  text: String
)

object GroupMeTextMessage {
  implicit val groupMeTextDecoder: Decoder[GroupMeTextMessage] = deriveDecoder[GroupMeTextMessage]

  implicit def groupMeTextEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, GroupMeTextMessage] =
    jsonOf[F, GroupMeTextMessage]

  implicit val groupMeTextEncoder: Encoder[GroupMeTextMessage] = deriveEncoder[GroupMeTextMessage]

  implicit def groupMeTextEntityEncoder[F[_]]: EntityEncoder[F, GroupMeTextMessage] =
    jsonEncoderOf[F, GroupMeTextMessage]
}

object BasicJson {
  implicit val unitDecoder: Decoder[Unit] = deriveDecoder[Unit]
  implicit def unitEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, Unit] = jsonOf[F, Unit]
}

trait GroupMeClient[F[_]] {

  def sendTextGroupMeMessage(message: String): F[Status]

}

class GroupMeClientImpl[F[_]: Concurrent](
  config: GroupMeConfig,
  client: Client[F]
) extends GroupMeClient[F] {
  val dsl = new Http4sClientDsl[F] {}

  override def sendTextGroupMeMessage(message: String): F[Status] = {
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
