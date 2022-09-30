package com.g5deathmarch.dndbot.groupme

import cats.effect.kernel.Concurrent
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import io.circe.Decoder.Result
import io.circe.generic.semiauto.deriveDecoder
import io.circe.Decoder
import org.http4s.circe.jsonOf
import org.http4s.EntityDecoder
import cats.effect.kernel.Async
import scala.util.Random
import cats.instances.unit

case class GroupMeRequestBody(
  group_id: String,
  id: String,
  name: String,
  sender_id: String,
  user_id: String,
  text: String
)

object GroupMeRequestBody {
  implicit val groupMeRequestDecoder: Decoder[GroupMeRequestBody] = deriveDecoder[GroupMeRequestBody]

  implicit def groupMeRequestEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, GroupMeRequestBody] =
    jsonOf[F, GroupMeRequestBody]

}

class GroupMeService[F[_]: Concurrent](config: GroupMeConfig, client: GroupMeClient[F]) {

  val dsl = new Http4sDsl[F] {}
  import dsl._

  private def handleDieRoll(times: Int, dieUnit: Int): F[Unit] = {
    val results: Seq[Int] = (0 until times).map { _ =>
      Random.nextInt(dieUnit) + 1
    }
    val output = results.fold(0)(_ + _)
    client.sendTextGroupMeMessage(
      s"""
      Result of ${times}d${dieUnit}:
      ${results.mkString(" + ")} = $output
      """
    ) >> Concurrent[F].unit
  }

  def routes: HttpRoutes[F] = {
    HttpRoutes.of[F] { case req @ POST -> Root =>
      req.decode[GroupMeRequestBody] { body =>
        body.text match {
          case s"/roll ${times}d${unit}" =>
            handleDieRoll(times.toInt, unit.toInt) >> Ok()
        }
      }
    }
  }
}
