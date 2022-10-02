package com.g5deathmarch.dndbot.github

import cats.effect.Concurrent
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Authorization

case class GithubIssueRequest(
  title: String,
  body: String
)

case class GithubIssueResponse(
  title: String,
  body: String,
  html_url: String
)

trait GithubClient[F[_]] {

  def createIssue(issueTitle: String, user: String): F[GithubIssueResponse]

}

class GithubClientImpl[F[_]: Concurrent](
  config: GithubConfig,
  client: Client[F]
) extends GithubClient[F] {
  val dsl = new Http4sClientDsl[F] {}

  private val apiRepoUrl: String = s"https://api.github.com/repos/${config.username}/${config.repoName}"

  override def createIssue(issueTitle: String, user: String): F[GithubIssueResponse] = {
    val uri = Uri.unsafeFromString(s"${apiRepoUrl}/issues")
    val req = Request[F](Method.POST, uri)
      .withHeaders(Authorization(BasicCredentials(config.username, config.accessToken)))
      .withEntity[GithubIssueRequest](
        GithubIssueRequest(
          issueTitle,
          s"Recommended by $user"
        )
      )

    client.expect[GithubIssueResponse](req)
  }

}
