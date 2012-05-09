package de.dimond.tippspiel.model

import org.joda.time.{DateTime, DateTimeZone}

import org.scala_tools.time.Imports._

object SpecialData {
  def init(forTesting: Boolean = false) {
    val teamList = List(SpecialAnswer("spain"),
                        SpecialAnswer("netherlands"),
                        SpecialAnswer("germany"),
                        SpecialAnswer("england"),
                        SpecialAnswer("italy"),
                        SpecialAnswer("croatia"),
                        SpecialAnswer("russia"),
                        SpecialAnswer("greece"),
                        SpecialAnswer("portugal"),
                        SpecialAnswer("sweden"),
                        SpecialAnswer("france"),
                        SpecialAnswer("denmark"),
                        SpecialAnswer("ukraine"),
                        SpecialAnswer("czech_republic"),
                        SpecialAnswer("ireland"),
                        SpecialAnswer("poland"))

    val utc2 = DateTimeZone.forOffsetHours(2)
    val euroStartDate = new DateTime(2012, 6, 8, 18, 00, utc2)

    val winnerSpecial = Special(1, "euro_winner_title", 15, teamList, euroStartDate)
    val secondSpecial = Special(2, "euro_runnerup_title", 5, teamList, euroStartDate)
    val topScorer = Special(3, "top_scorer_title", 5, teamList, euroStartDate)
  }
}
