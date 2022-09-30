package com.g5deathmarch.dndbot

import cats.effect.{ExitCode, IO, IOApp}

object Main {
  def run(args: List[String]) =
    Server.stream[IO].compile.drain.as(ExitCode.Success)
}
