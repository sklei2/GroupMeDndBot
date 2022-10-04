package com.g5deathmarch.dndbot.fantasynamegenerator

import com.typesafe.scalalogging.StrictLogging
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

import java.util.logging.Level

class FantasyNameGeneratorScraper extends StrictLogging {

  private val browser: HtmlUnitBrowser = {
    // I DO NOT CARE ABOUT YOUR PROBLEMS HTMLUNIT
    java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF)
    HtmlUnitBrowser.typed()
  }
  private val buttonsSelector = "#nameGen input[type='button']"
  private val namesSelector = "#result"

  def getNames(race: String): Set[String] = {
    val document = browser.get(FantasyNameGeneratorScraper.url(race))

    val names = document >> element(namesSelector)

    names.innerHtml.split("<br></br>").take(5).toSet

  }
}

object FantasyNameGeneratorScraper {
  def url(race: String): String = s"https://fantasynamegenerators.com/dnd-${race.toLowerCase}-names.php"
}
