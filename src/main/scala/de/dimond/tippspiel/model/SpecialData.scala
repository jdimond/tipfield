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

    val groupATeams = List(SpecialAnswer("poland"),
                           SpecialAnswer("greece"),
                           SpecialAnswer("russia"),
                           SpecialAnswer("czech_republic"))

    val groupBTeams = List(SpecialAnswer("netherlands"),
                           SpecialAnswer("denmark"),
                           SpecialAnswer("germany"),
                           SpecialAnswer("portugal"))

    val groupCTeams = List(SpecialAnswer("spain"),
                           SpecialAnswer("italy"),
                           SpecialAnswer("ireland"),
                           SpecialAnswer("croatia"))

    val groupDTeams = List(SpecialAnswer("ukraine"),
                           SpecialAnswer("sweden"),
                           SpecialAnswer("france"),
                           SpecialAnswer("england"))

    val utc2 = DateTimeZone.forOffsetHours(2)
    val euroStartDate = new DateTime(2012, 6, 8, 18, 00, utc2)

    val winnerSpecial = Special(1, "euro_winner_title", 7, teamList, euroStartDate)
    val topScorer = Special(2, "top_scorer_title", 3, teamList, euroStartDate)
    val groupA = Special(3, "special_groupa_title", 3, groupATeams, euroStartDate)
    val groupB = Special(4, "special_groupb_title", 3, groupBTeams, euroStartDate)
    val groupC = Special(5, "special_groupc_title", 3, groupCTeams, euroStartDate)
    val groupD = Special(6, "special_groupd_title", 3, groupDTeams, euroStartDate)

    //val secondSpecial = Special(2, "euro_runnerup_title", 5, teamList, euroStartDate)
  }
}
