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
import com.g5deathmarch.dndbot.github.GithubClient
import com.g5deathmarch.dndbot.github.GithubIssue

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

class GroupMeService[F[_]: Concurrent](
  config: GroupMeConfig,
  groupmeClient: GroupMeClient[F],
  githubClient: GithubClient[F]
) {

  val dsl = new Http4sDsl[F] {}
  import dsl._

  private def handleHelp: F[Unit] = {
    val helpText: Iterator[String] = Source.fromResource("help.txt").getLines()
    groupmeClient.sendTextGroupMeMessage(helpText.mkString("\n")) >> Concurrent[F].unit
  }

  type Sides = Int
  type RollCount = Int

  private def handleDieRoll(dieRolls: Seq[(RollCount, Sides)]): F[Unit] = {
    // If the text gives us multiple of the same die sides, let's just group those all together.
    val groupedDieRolls: Seq[(RollCount, Sides)] = dieRolls
      .groupBy(_._2)
      .map { case (sides, allRollsForSide) => (allRollsForSide.map(_._1).fold(0)(_ + _), sides) }
      .toSeq

    val results: Seq[Seq[Int]] = groupedDieRolls
      .map { case (rollCount, sides) =>
        (0 until rollCount).map { _ => Random.nextInt(sides) + 1 }.toSeq
      }

    val message: String = {
      val header =
        groupedDieRolls.map { case (rollCount, sides) => s"${rollCount}d${sides}" }.mkString("Result of ", " + ", "")
      val body = {
        val total = results.flatten.fold(0)(_ + _)
        // if we're only rolling a single die, just show the result of that one die without anything else.
        if (results.flatten.size == 1)
          s"${total}"
        else {
          val rolls =
            results
              .map { result => result.mkString("(", " + ", ")") }
              .mkString(" + ")

          s"${rolls} = ${total}"
        }
      }

      s"$header\n$body"
    }

    groupmeClient.sendTextGroupMeMessage(message) >> Concurrent[F].unit
  }

  private def handleIdea(idea: String, user: String): F[Unit] = {
    githubClient.createIssue(idea, user).flatMap { createdIssue =>
      val message = createdIssue.url match {
        case Some(url) =>
          s"I've let my creators know about your idea! Check it out here: ${url}"
        case None =>
          "I've tried to let them know about your idea, but failed :("
      }
      groupmeClient.sendTextGroupMeMessage(message)

    } >> Concurrent[F].unit
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
          case s"/idea ${idea}" =>
            handleIdea(idea, body.name) >> Ok()
        }
      }
    }
  }
}
