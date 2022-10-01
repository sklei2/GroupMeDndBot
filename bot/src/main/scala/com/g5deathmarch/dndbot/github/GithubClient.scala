package com.g5deathmarch.dndbot.github

import cats.effect.Concurrent
import cats.implicits._
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import io.circe.generic.auto._
import org.http4s.headers.Authorization

case class GithubIssue(
  title: String,
  body: String
)

trait GithubClient[F[_]] {

  def createIssue(issueTitle: String, user: String): F[Status]

}

class GithubClientImpl[F[_]: Concurrent](
  config: GithubConfig,
  client: Client[F]
) extends GithubClient[F] {
  val dsl = new Http4sClientDsl[F] {}

  private val apiRepoUrl: String = s"https://api.github.com/repos/${config.username}/${config.repoName}"

  override def createIssue(issueTitle: String, user: String): F[Status] = {
    val uri = Uri.unsafeFromString(s"${apiRepoUrl}/issues")
    val req = Request[F](Method.POST, uri)
      .withHeaders(Authorization(BasicCredentials(config.username, config.accessToken)))
      .withEntity[GithubIssue](
        GithubIssue(
          issueTitle,
          s"Recommended by ${user}"
        )
      )

    client.status(req)
  }

}
