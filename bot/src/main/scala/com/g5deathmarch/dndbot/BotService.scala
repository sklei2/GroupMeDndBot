package com.g5deathmarch.dndbot

import cats.effect.kernel.Concurrent
import cats.implicits._
import com.g5deathmarch.dndbot.fantasynamegenerator.{FantasyNameGeneratorScraper, Gender, Race}
import com.g5deathmarch.dndbot.github.GithubClient
import com.g5deathmarch.dndbot.groupme.{GroupMeClient, GroupMeConfig}
import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.eclipse.jetty.util.resource.Resource
import org.http4s.circe.jsonOf
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes, Status}

import java.io.FileNotFoundException
import scala.io.Source
import scala.util.Random
import scala.util.matching.Regex

case class GroupMeRequestBody(
  group_id: String,
  id: String,
  name: String,
  sender_id: String,
  user_id: String,
  text: String,
  sender_type: String
)

object GroupMeRequestBody {
  implicit val groupMeRequestDecoder: Decoder[GroupMeRequestBody] = deriveDecoder[GroupMeRequestBody]

  implicit def groupMeRequestEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, GroupMeRequestBody] =
    jsonOf[F, GroupMeRequestBody]

}

class BotService[F[_]: Concurrent](
  config: GroupMeConfig,
  groupmeClient: GroupMeClient[F],
  githubClient: GithubClient[F],
  fantasyNameScraper: FantasyNameGeneratorScraper
) extends StrictLogging {

  val dsl = new Http4sDsl[F] {}
  import dsl._
  // Type alias to make it easier to understand.
  type Sides = Int
  type RollCount = Int
  // Regex to capture [number_of_rolls]d[number_of_sides]
  val diceRollRegex: Regex = raw"(\d+)d(\d+)".r

  def routes: HttpRoutes[F] = {
    HttpRoutes.of[F] { case req @ POST -> Root =>
      req.decode[GroupMeRequestBody] { body =>
        // if started with a `/` let's listen in. If not we don't care
        if (body.sender_type != "bot" && body.text.toLowerCase.startsWith("/")) {
          logger.debug(s"Attempting to handle command=${body.text}")
          val action = body.text.toLowerCase.trim match {
            case s"/help ${command}" if command.toLowerCase.replaceFirst("/", "") != "help" =>
              // Make sure that the resource text files have the command name without the `/`
              val commandName = command.toLowerCase.replaceFirst("/", "")
              try {
                val message: String = {
                  val textFileContent = Source.fromResource(s"${commandName}.txt").getLines().mkString("\n")
                  if (commandName == "name") {
                    s"$textFileContent\n\nI support the following Races:\n${Race.values.map(_.toString).mkString("\n")}"
                  } else {
                    textFileContent
                  }
                }

                groupmeClient.sendTextGroupMeMessage(message)
              } catch {
                case _: FileNotFoundException =>
                  groupmeClient.sendTextGroupMeMessage(s"I don't know how to help with the '$command' command :(")
              }
            case s"/help" =>
              logger.debug("Handling '/help' command")
              handleHelp
            case s"/roll ${rest}" =>
              logger.debug(s"Handling '/roll'. args=$rest")
              // parse the rest of the string into objects
              handleDieMath(rest)
            case s"/idea ${title}" =>
              logger.debug(s"Handling '/idea'. title=$title user=${body.name}")
              handleIdea(title, body.name)
            case s"/name ${race} ${gender}" =>
              logger.debug(s"Handling '/name' with race=$race gender=$gender")
              handleName(race, Some(gender))
            case s"/name ${race}" =>
              logger.debug(s"Handling '/name' with race=$race")
              handleName(race, None)
            case _ =>
              logger.error(s"Unable to handle command: ${body.text.toLowerCase}")
              groupmeClient.sendTextGroupMeMessage("I'm sorry. I'm not sure what you wanted me to do :(")
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
    val message: String = Source.fromResource("help.txt").getLines().mkString("\n")
    groupmeClient.sendTextGroupMeMessage(message) >> Concurrent[F].unit
  }

  private def handleDieMath(arithmetic: String): F[Unit] = {
    type Result = Int
    type Message = String

    def handleValue(v: String): (Result, Message) = {
      diceRollRegex.findFirstMatchIn(v) match {
        case Some(regexMatch) =>
          val rollCount = regexMatch.subgroups.head.toInt
          val sides = regexMatch.subgroups(1).toInt
          val results = (0 until rollCount).map { _ => Random.nextInt(sides) + 1 }
          (results.sum, results.mkString("(", " + ", ")"))
        case None => (v.toInt, v)
      }
    }

    def recursiveMath(expressions: List[String], resultAccumulator: Int, messageAccumulator: String, firstRun: Boolean): (Result, Message) = {
      expressions match {
        case _ :: Nil =>
          (resultAccumulator, messageAccumulator)
        case l :: "+" :: r :: rest =>
          val (lResult, lMessage) = handleValue(l)
          val (rResult, rMessage) = handleValue(r)

          if (firstRun) {
            recursiveMath(s"${lResult + rResult}" :: rest, lResult + rResult, s"$lMessage + $rMessage", false)
          } else {
            recursiveMath(s"${lResult + rResult}" :: rest, lResult + rResult, s"$messageAccumulator + $rMessage", false)
          }

        case l :: "-" :: r :: rest =>
          val (lResult, lMessage) = handleValue(l)
          val (rResult, rMessage) = handleValue(r)

          if (firstRun) {
            recursiveMath(s"${lResult - rResult}" :: rest, lResult - rResult, s"$lMessage - $rMessage", false)
          } else {
            recursiveMath(s"${lResult - rResult}" :: rest, lResult - rResult, s"$messageAccumulator - $rMessage", false)
          }
      }
    }

    val message = {
      val expressions: List[String] = arithmetic.toLowerCase.split("\\s").toList
      val header = s"Result of $arithmetic"
      val (result, mathMessage) = recursiveMath(expressions, 0, "", true)
      val body = s"$mathMessage = $result"
      s"$header\n$body"
    }

    groupmeClient.sendTextGroupMeMessage(message) >> Concurrent[F].unit
  }

  private def handleIdea(idea: String, user: String): F[Unit] = {
    githubClient.createIssue(idea, user).flatMap { createdIssue =>
      val message = s"I've let my creators know about your idea! Check it out here: ${createdIssue.html_url}"
      groupmeClient.sendTextGroupMeMessage(message)
    } >> Concurrent[F].unit
  }

  private def handleName(race: String, gender: Option[String]): F[Unit] = {
    val message = (Race.valueOf(race), gender) match {
      case (None, _) =>
        s"I'm sorry I don't support '$race' as a fantasy race to generate names from. Please use '/help names' to get info as to what I know."
      case (Some(r), Some(g)) if Gender.valueOf(g).isDefined =>
        val genderEnum = Gender.valueOf(g)
        val names = fantasyNameScraper.getNames(r, genderEnum)
        names.mkString("\n")
      case (Some(r), None) =>
        val names = fantasyNameScraper.getNames(r, None)
        names.mkString("\n")
      case (Some(_), Some(g)) =>
        s"I'm sorry I don't support $g as a gender option."
    }

    groupmeClient.sendTextGroupMeMessage(message) >> Concurrent[F].unit
  }
}
