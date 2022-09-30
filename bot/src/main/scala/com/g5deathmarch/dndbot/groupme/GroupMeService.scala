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
import scala.io.Source

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

  private def handleHelp: F[Unit] = {
    val helpText: Iterator[String] = Source.fromResource("help.txt").getLines()
    client.sendTextGroupMeMessage(helpText.mkString("\n")) >> Concurrent[F].unit
  }

  private def handleDieRoll(dieRolls: Seq[(Int, Int)]): F[Unit] = {
    println(dieRolls)
    val subMessageAndTotal: Seq[(String, Int)] = dieRolls
      .map { case (times, dieUnit) =>
        val results = (0 until times).map { _ => Random.nextInt(dieUnit) + 1 }.toSeq
        val output = results.fold(0)(_ + _)
        (
          s"""
      Result of ${times}d${dieUnit}:
      ${results.mkString(" + ")}${if (times > 1) s"= $output" else ""}
      """,
          output
        )
      }

    val message: String = {
      val total = subMessageAndTotal.foldLeft(0) { case (count, messageAndTotal) => count + messageAndTotal._2 }
      val subMessage = subMessageAndTotal.map(_._1).mkString("\n")
      if (subMessageAndTotal.size > 1) {
        s"""
      $subMessage
      Total: $total
      """
      } else {
        subMessage
      }

    }

    client.sendTextGroupMeMessage(message) >> Concurrent[F].unit
  }

  // Regex to capture [number_of_rolls]d[number_of_sides]
  val diceRollRegex = raw"(\d+)d(\d+)\s?\+?".r

  def routes: HttpRoutes[F] = {
    HttpRoutes.of[F] { case req @ POST -> Root =>
      req.decode[GroupMeRequestBody] { body =>
        body.text match {
          case s"/help" =>
            handleHelp >> Ok()
          case s"/roll ${rest}" if diceRollRegex.findFirstIn(rest).nonEmpty =>
            val dieRolls =
              diceRollRegex.findAllIn(rest).matchData.map { m => (m.subgroups(0).toInt, m.subgroups(1).toInt) }.toSeq
            handleDieRoll(dieRolls) >> Ok()
        }
      }
    }
  }
}
