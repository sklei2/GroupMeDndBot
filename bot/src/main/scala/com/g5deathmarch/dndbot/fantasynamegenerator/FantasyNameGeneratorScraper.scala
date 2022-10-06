package com.g5deathmarch.dndbot.fantasynamegenerator

import com.typesafe.scalalogging.StrictLogging
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser.{HtmlUnitDocument, HtmlUnitElement}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

import java.util.logging.Level

case class FantasySearchOptions(race: Race.RaceType, gendered: Boolean)

class FantasyNameGeneratorScraper extends StrictLogging {

  private val browser: HtmlUnitBrowser = {
    // I DO NOT CARE ABOUT YOUR PROBLEMS HTMLUNIT
    java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF)
    HtmlUnitBrowser.typed()
  }

  private val namesSelector = "#result"

  def getNames(race: Race.RaceType, gender: Option[Gender.GenderType]): Set[String] = {
    val document: HtmlUnitDocument = browser.get(FantasyNameGeneratorScraper.url(race))

    val names = (gender, FantasyNameGeneratorScraper.gendered.getOrElse(race, false)) match {
      case (None, _) | (_, false) =>
        // Not gendered, so just grab whatever the page has
        document >> element(namesSelector)
      case (Some(gender), true) =>
        val buttons: List[HtmlUnitElement] = document >> pElementList("#nameGen input") match {
          case b: List[HtmlUnitElement] => b
          case _ => List.empty
        }
        buttons.find { b =>
          val value = b.attr("value").toLowerCase
          // Human buttons specifically have `(Old) Male Names` and `(Old) Female Names` for some reason.
          // So let's make sure that A) we select the button has the gender and it isn't one of the outlier ones.
          value.contains(gender.toString.toLowerCase) && !value.contains("Old")
        } match {
          case Some(el) =>
            el.underlying.click[com.gargoylesoftware.htmlunit.Page]()
          case None =>
            throw new Exception("I CAN'T FIND THIS GENDER OPTION!")
        }
        document >> element(namesSelector)
    }

    names.innerHtml.split("<br></br>").take(5).toSet

  }
}

object FantasyNameGeneratorScraper {
  val gendered: Map[Race.RaceType, Boolean] = Map(
    Race.aarakocra -> false,
    Race.aasimar -> true,
    Race.bugbear -> false,
    Race.dragonborn -> true,
    Race.drow -> true,
    Race.dwarf -> true,
    Race.elf -> true,
    Race.eladrin -> true,
    Race.firbolg -> true,
    Race.genasi -> false,
    Race.gnome -> true,
    Race.goblin -> true,
    Race.goliath -> true,
    Race.halfElf -> true,
    Race.halfOrc -> true,
    Race.halfling -> true,
    Race.hobgoblin -> false,
    Race.human -> true,
    Race.kenku -> false,
    Race.kobold -> false,
    Race.lizardfolk -> false,
    Race.tortle -> false,
    Race.tiefling -> true,
    Race.orc -> true
  )

  def url(race: Race.RaceType): String = s"https://fantasynamegenerators.com/dnd-${race.toString.toLowerCase}-names.php"
}

object Gender extends Enumeration {
  type GenderType = Value
  val female: GenderType = Value("female")
  val male: GenderType = Value("male")
  def valueOf(name: String): Option[Value] = this.values.find(_.toString.toLowerCase == name.toLowerCase)
}

object Race extends Enumeration {
  type RaceType = Value
  val aarakocra: RaceType = Value("Aarakocra")
  val aasimar: RaceType = Value("Aasimar")
  val bugbear: RaceType = Value("Bugbear")
  val dragonborn: RaceType = Value("Dragonborn")
  val drow: RaceType = Value("Drow")
  val dwarf: RaceType = Value("Dwarf")
  val elf: RaceType = Value("Elf")
  val eladrin: RaceType = Value("Eladrin")
  val firbolg: RaceType = Value("Firbolg")
  val genasi: RaceType = Value("Genasi")
  val gnome: RaceType = Value("Gnome")
  val goblin: RaceType = Value("Goblin")
  val goliath: RaceType = Value("Goliath")
  val halfElf: RaceType = Value("Half-Elf")
  val halfOrc: RaceType = Value("Half-Orc")
  val halfling: RaceType = Value("Halfling")
  val hobgoblin: RaceType = Value("Hobgoblin")
  val human: RaceType = Value("Human")
  val kenku: RaceType = Value("Kenku")
  val kobold: RaceType = Value("Kobold")
  val lizardfolk: RaceType = Value("Lizardfolk")
  val tortle: RaceType = Value("Tortle")
  val tiefling: RaceType = Value("Tiefling")
  val orc: RaceType = Value("Orc")

  def valueOf(name: String): Option[Value] = this.values.find(_.toString.toLowerCase == name.toLowerCase)
}
