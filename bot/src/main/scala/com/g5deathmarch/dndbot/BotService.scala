package com.g5deathmarch.dndbot

import cats.effect.kernel.Concurrent
import cats.implicits._
import com.g5deathmarch.dndbot.github.GithubClient
import com.g5deathmarch.dndbot.groupme.{GroupMeClient, GroupMeConfig}
import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.circe.jsonOf
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}

import scala.io.Source
import scala.util.Random

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

class BotService[F[_]: Concurrent](
  config: GroupMeConfig,
  groupmeClient: GroupMeClient[F],
  githubClient: GithubClient[F]
) extends StrictLogging {

  val dsl = new Http4sDsl[F] {}
  import dsl._
  // Type alias to make it easier to understand.
  type Sides = Int
  type RollCount = Int
  // Regex to capture [number_of_rolls]d[number_of_sides]
  val diceRollRegex = raw"(\d+)d(\d+)\s?\+?".r

  def routes: HttpRoutes[F] = {
    HttpRoutes.of[F] { case req @ POST -> Root =>
      req.decode[GroupMeRequestBody] { body =>
        // if started with a `/` let's listen in. If not we don't care
        if (body.text.toLowerCase.startsWith("/")) {
          logger.debug(s"Attempting to handle command=${body.text}")
          val action = body.text match {
            case s"/help" =>
              logger.debug("Handling '/help' command")
              handleHelp
            case s"/roll ${rest}" if diceRollRegex.findFirstIn(rest).nonEmpty =>
              logger.debug(s"Handling '/roll'. args=$rest")
              val dieRolls =
                diceRollRegex.findAllIn(rest).matchData.map { m => (m.subgroups(0).toInt, m.subgroups(1).toInt) }.toSeq
              handleDieRoll(dieRolls)
            case s"/idea ${title}" =>
              logger.debug(s"Handling '/idea'. title=$title user=${body.name}")
              handleIdea(title, body.name)
            case _ =>
              logger.error(s"Unable to handle command: ${body.text.toLowerCase}")
              groupmeClient.sendTextGroupMeMessage("I'm sorry.I'm not sure what you wanted me to do :(")
          }
          action >> Ok()
        } else {
          // if we don't start with the activation character, don't do anything.
          // Just OK it.
          Ok()
        }
      }
    }
  }

  private def handleHelp: F[Unit] = {
    val helpText: Iterator[String] = Source.fromResource("help.txt").getLines()
    groupmeClient.sendTextGroupMeMessage(helpText.mkString("\n")) >> Concurrent[F].unit
  }

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
}