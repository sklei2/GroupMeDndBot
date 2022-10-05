package com.g5deathmarch.dndbot.fantasynamegenerator

import com.typesafe.scalalogging.StrictLogging
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

import java.util.logging.Level

case class FantasySearchOptions(race: Race.RaceType, gendered: Boolean, additionalButtons: Set[String]=Set.empty)

class FantasyNameGeneratorScraper extends StrictLogging {

  private val browser: HtmlUnitBrowser = {
    // I DO NOT CARE ABOUT YOUR PROBLEMS HTMLUNIT
    java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF)
    HtmlUnitBrowser.typed()
  }
  private val buttonsSelector = "#nameGen input[type='button']"
  private val namesSelector = "#result"

  def getNames(race: Race.RaceType): Set[String] = {
    val document = browser.get(FantasyNameGeneratorScraper.url(race))

    val names = document >> element(namesSelector)

    names.innerHtml.split("<br></br>").take(5).toSet

  }
}

object FantasyNameGeneratorScraper {
  def url(race: Race.RaceType): String = s"https://fantasynamegenerators.com/dnd-${race.toString.toLowerCase}-names.php"

  val options: Set[FantasySearchOptions] = Set(
    FantasySearchOptions(Race.aarakocra, gendered = false),
    FantasySearchOptions(Race.aasimar, gendered = true),
    FantasySearchOptions(Race.bugbear, gendered = false),
    FantasySearchOptions(Race.dragonborn, gendered = true, additionalButtons = Set("Childhood")),
    FantasySearchOptions(Race.drow, gendered = true),
    FantasySearchOptions(Race.dwarf, gendered = true),
    FantasySearchOptions(Race.elf, gendered = true, additionalButtons = Set("Child")),
    FantasySearchOptions(Race.eladrin, gendered = true),
    FantasySearchOptions(Race.firbolg, gendered = true),
    FantasySearchOptions(Race.genasi, gendered = false),
    FantasySearchOptions(Race.gnome, gendered = true),
    FantasySearchOptions(Race.goblin, gendered = true),
    FantasySearchOptions(Race.goliath, gendered = true),
    FantasySearchOptions(Race.halfElf, gendered = true),
    FantasySearchOptions(Race.halfOrc, gendered = true),
    FantasySearchOptions(Race.halfling, gendered = true),
    FantasySearchOptions(Race.hobgoblin, gendered = false),
    FantasySearchOptions(Race.human, gendered = true, additionalButtons = Set("(Old) Male", "(Old) Female")),
    FantasySearchOptions(Race.kenku, gendered = false),
    FantasySearchOptions(Race.kobold, gendered = false),
    FantasySearchOptions(Race.lizardfolk, gendered = false, additionalButtons = Set("Meanings")),
    FantasySearchOptions(Race.tortle, gendered = false),
    FantasySearchOptions(Race.tiefling, gendered = true, additionalButtons = Set("Virtue")),
    FantasySearchOptions(Race.orc, gendered = true),
  )
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
