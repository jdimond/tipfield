package de.dimond.tippspiel.model.wc2014

import de.dimond.tippspiel.model._

import org.joda.time.{DateTime, DateTimeZone}

import org.scala_tools.time.Imports._

object SpecialData {
  def init(forTesting: Boolean = false) {
    val teamList = List(SpecialAnswer("brazil"),
                        SpecialAnswer("croatia"),
                        SpecialAnswer("mexico"),
                        SpecialAnswer("cameroon"),
                        SpecialAnswer("spain"),
                        SpecialAnswer("netherlands"),
                        SpecialAnswer("chile"),
                        SpecialAnswer("australia"),
                        SpecialAnswer("colombia"),
                        SpecialAnswer("greece"),
                        SpecialAnswer("ivory_coast"),
                        SpecialAnswer("japan"),
                        SpecialAnswer("uruguay"),
                        SpecialAnswer("costa_rica"),
                        SpecialAnswer("england"),
                        SpecialAnswer("italy"),
                        SpecialAnswer("switzerland"),
                        SpecialAnswer("ecuador"),
                        SpecialAnswer("france"),
                        SpecialAnswer("honduras"),
                        SpecialAnswer("argentina"),
                        SpecialAnswer("bosnia_and_herzegovina"),
                        SpecialAnswer("iran"),
                        SpecialAnswer("nigeria"),
                        SpecialAnswer("germany"),
                        SpecialAnswer("portugal"),
                        SpecialAnswer("ghana"),
                        SpecialAnswer("united_states"),
                        SpecialAnswer("belgium"),
                        SpecialAnswer("algeria"),
                        SpecialAnswer("russia"),
                        SpecialAnswer("south_korea"))

    val groupAList = List(SpecialAnswer("brazil"),
                          SpecialAnswer("croatia"),
                          SpecialAnswer("mexico"),
                          SpecialAnswer("cameroon"))
    val groupBList = List(SpecialAnswer("spain"),
                          SpecialAnswer("netherlands"),
                          SpecialAnswer("chile"),
                          SpecialAnswer("australia"))
    val groupCList = List(SpecialAnswer("colombia"),
                          SpecialAnswer("greece"),
                          SpecialAnswer("ivory_coast"),
                          SpecialAnswer("japan"))
    val groupDList = List(SpecialAnswer("uruguay"),
                          SpecialAnswer("costa_rica"),
                          SpecialAnswer("england"),
                          SpecialAnswer("italy"))
    val groupEList = List(SpecialAnswer("switzerland"),
                          SpecialAnswer("ecuador"),
                          SpecialAnswer("france"),
                          SpecialAnswer("honduras"))
    val groupFList = List(SpecialAnswer("argentina"),
                          SpecialAnswer("bosnia_and_herzegovina"),
                          SpecialAnswer("iran"),
                          SpecialAnswer("nigeria"))
    val groupGList = List(SpecialAnswer("germany"),
                          SpecialAnswer("portugal"),
                          SpecialAnswer("ghana"),
                          SpecialAnswer("united_states"))
    val groupHList = List(SpecialAnswer("belgium"),
                          SpecialAnswer("algeria"),
                          SpecialAnswer("russia"),
                          SpecialAnswer("south_korea"))

    val utc3 = DateTimeZone.forOffsetHours(-3)
    val wcStartDate = new DateTime(2014, 6, 12, 17, 00, utc3)

    if (forTesting) {
      val winnerSpecial = Special(1, "wc_winner_title", 10, teamList, DateTime.now)
      val topScorer = Special(2, "top_scorer_title", 3, teamList, DateTime.now + 1.minutes)
      val groupA = Special(3, "special_groupa_title", 3, groupAList, DateTime.now + 2.minutes)
      val groupB = Special(4, "special_groupb_title", 3, groupBList, DateTime.now + 3.minutes)
      val groupC = Special(5, "special_groupc_title", 3, groupCList, DateTime.now + 4.minutes)
      val groupD = Special(6, "special_groupd_title", 3, groupDList, DateTime.now + 5.minutes)
      val groupE = Special(7, "special_groupe_title", 3, groupEList, DateTime.now + 6.minutes)
      val groupF = Special(8, "special_groupf_title", 3, groupFList, DateTime.now + 7.minutes)
      val groupG = Special(9, "special_groupg_title", 3, groupGList, DateTime.now + 8.minutes)
      val groupH = Special(10, "special_grouph_title", 3, groupHList, DateTime.now + 9.minutes)
    } else {
      val winnerSpecial = Special(1, "wc_winner_title", 10, teamList, wcStartDate)
      val topScorer = Special(2, "top_scorer_title", 3, teamList, wcStartDate)
      val groupA = Special(3, "special_groupa_title", 3, groupAList, wcStartDate)
      val groupB = Special(4, "special_groupb_title", 3, groupBList, wcStartDate)
      val groupC = Special(5, "special_groupc_title", 3, groupCList, wcStartDate)
      val groupD = Special(6, "special_groupd_title", 3, groupDList, wcStartDate)
      val groupE = Special(7, "special_groupe_title", 3, groupEList, wcStartDate)
      val groupF = Special(8, "special_groupf_title", 3, groupFList, wcStartDate)
      val groupG = Special(9, "special_groupg_title", 3, groupGList, wcStartDate)
      val groupH = Special(10, "special_grouph_title", 3, groupHList, wcStartDate)
    }

    //val secondSpecial = Special(2, "euro_runnerup_title", 5, teamList, wcStartDate)
  }
}
