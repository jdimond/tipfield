package de.dimond.tippspiel.model.wc2014

import de.dimond.tippspiel.model._

import org.joda.time.{DateTime, DateTimeZone}

import org.scala_tools.time.Imports._

object GameData {

  case class WorldCupTeam(name: String, emblemUrl: String) extends Team

  case class WorldCupGroup(name: String, teams: Seq[Team], games: Seq[Game]) extends Group {
    override def standings: Seq[Standing] = {
      val unsorted = this.unsortedStandings
      lazy val standingsTieBreaker = Map() ++ unsorted.groupBy(
          s => (s.points,s.goalDifference,s.goalsScored)
        ).values.filter(_.size > 1).map(x => {
          val teams = x.map(s => s.team).toList
          val subGroup = WorldCupGroup("", teams, games.filter(g => (g.teamAway.team, g.teamHome.team) match {
              case (Right(a), Right(b)) => teams.contains(a) && teams.contains(b)
              case _ => false
            }))
          subGroup.unsortedStandings.map(s => (s.team, s))
        }).flatten

      unsorted.toList.sortWith((x, y) => {
        x.compareTo(y) match {
          case x if x < 0 => true
          case x if x > 0 => false
          case 0 => {
            val tieBreaker = standingsTieBreaker
            println(tieBreaker(x.team))
            println(tieBreaker(y.team))
            tieBreaker(x.team).compareTo(tieBreaker(y.team)) < 0
          }
        }
      })
    }
  }

  def init(forTesting: Boolean = false) {
    def createDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int, timeZone: DateTimeZone) = {
      if (forTesting) {
        DateTime.now + (day - 15).minutes + (month - 6).hours
      } else {
        new DateTime(year, month, day, hour, minute, timeZone)
      }
    }
    // Group A
    val brazil = WorldCupTeam("brazil", "brazil.png")
    val croatia = WorldCupTeam("croatia", "croatia.png")
    val mexico = WorldCupTeam("mexico", "mexico.png")
    val cameroon = WorldCupTeam("cameroon", "cameroon.png")

    // Group B
    val spain = WorldCupTeam("spain", "spain.png")
    val netherlands = WorldCupTeam("netherlands", "netherlands.png")
    val chile = WorldCupTeam("chile", "chile.png")
    val australia = WorldCupTeam("australia", "australia.png")

    // Group C
    val colombia = WorldCupTeam("colombia", "colombia.png")
    val greece = WorldCupTeam("greece", "greece.png")
    val ivoryCoast = WorldCupTeam("ivory_coast", "ivory_coast.png")
    val japan = WorldCupTeam("japan", "japan.png")

    // Group D
    val uruguay = WorldCupTeam("uruguay", "uruguay.png")
    val costaRica = WorldCupTeam("costa_rica", "costa_rica.png")
    val england = WorldCupTeam("england", "england.png")
    val italy = WorldCupTeam("italy", "italy.png")

    // Group E
    val switzerland = WorldCupTeam("switzerland", "switzerland.png")
    val ecuador = WorldCupTeam("ecuador", "ecuador.png")
    val france = WorldCupTeam("france", "france.png")
    val honduras = WorldCupTeam("honduras", "honduras.png")

    // Group F
    val argentina = WorldCupTeam("argentina", "argentina.png")
    val bosniaAndHerzegovina = WorldCupTeam("bosnia_and_herzegovina", "bosnia_and_herzegovina.png")
    val iran = WorldCupTeam("iran", "iran.png")
    val nigeria = WorldCupTeam("nigeria", "nigeria.png")

    // Group G
    val germany = WorldCupTeam("germany", "germany.png")
    val portugal = WorldCupTeam("portugal", "portugal.png")
    val ghana = WorldCupTeam("ghana", "ghana.png")
    val unitedStates = WorldCupTeam("united_states", "united_states.png")

    // Group H
    val belgium = WorldCupTeam("belgium", "belgium.png")
    val algeria = WorldCupTeam("algeria", "algeria.png")
    val russia = WorldCupTeam("russia", "russia.png")
    val southKorea = WorldCupTeam("south_korea", "south_korea.png")

    val utc3 = DateTimeZone.forOffsetHours(-3)

    val rioDeJaneiro = Location("rio_de_janeiro")
    val brasilia = Location("brasilia")
    val saoPaulo = Location("sao_paulo")
    val fortaleza = Location("fortaleza")

    val beloHorizonte = Location("belo_horizonte")
    val portoAlegre = Location("porto_alegre")
    val salvador = Location("salvador")
    val recife = Location("recife")

    val cuiaba = Location("cuiaba")
    val manaus = Location("manaus")
    val natal = Location("natal")
    val curitiba = Location("curitiba")

    def june(day: Int, hour: Int, minutes: Int) = createDateTime(2014, 6, day, hour, minutes, utc3)
    def july(day: Int, hour: Int, minutes: Int) = createDateTime(2014, 7, day, hour, minutes, utc3)

    val game01 = Game(1, brazil.reference, croatia.reference, june(12,17,00), saoPaulo)
    val game02 = Game(2, mexico.reference, cameroon.reference, june(13,13,00), natal)
    val game03 = Game(3, spain.reference, netherlands.reference, june(13,16,00), salvador)
    val game04 = Game(4, chile.reference, australia.reference, june(13,19,00), cuiaba)
    val game05 = Game(5, colombia.reference, greece.reference, june(14,13,00), beloHorizonte)
    val game06 = Game(6, ivoryCoast.reference, japan.reference, june(14,22,00), recife)
    val game07 = Game(7, uruguay.reference, costaRica.reference, june(14,16,00), fortaleza)
    val game08 = Game(8, england.reference, italy.reference, june(14,19,00), manaus)
    val game09 = Game(9, switzerland.reference, ecuador.reference, june(15,13,00), brasilia)
    val game10 = Game(10, france.reference, honduras.reference, june(15,16,00), portoAlegre)
    val game11 = Game(11, argentina.reference, bosniaAndHerzegovina.reference, june(15,19,00), rioDeJaneiro)
    val game12 = Game(12, iran.reference, nigeria.reference, june(16,16,00), curitiba)
    val game13 = Game(13, germany.reference, portugal.reference, june(16,13,00), salvador)
    val game14 = Game(14, ghana.reference, unitedStates.reference, june(16,19,00), natal)
    val game15 = Game(15, belgium.reference, algeria.reference, june(17,13,00), beloHorizonte)
    val game16 = Game(16, russia.reference, southKorea.reference, june(17,19,00), cuiaba)

    val game17 = Game(17, brazil.reference, mexico.reference, june(17,16,00), fortaleza)
    val game18 = Game(18, cameroon.reference, croatia.reference, june(18,19,00), manaus)
    val game19 = Game(19, spain.reference, chile.reference, june(18,16,00), rioDeJaneiro)
    val game20 = Game(20, australia.reference, netherlands.reference, june(18,13,00), portoAlegre)
    val game21 = Game(21, colombia.reference, ivoryCoast.reference, june(14,13,00), brasilia)
    val game22 = Game(22, japan.reference, greece.reference, june(19,19,00), natal)
    val game23 = Game(23, uruguay.reference, england.reference, june(19,16,00), saoPaulo)
    val game24 = Game(24, italy.reference, england.reference, june(20,13,00), recife)
    val game25 = Game(25, switzerland.reference, france.reference, june(20,16,00), salvador)
    val game26 = Game(26, honduras.reference, ecuador.reference, june(20,19,00), curitiba)
    val game27 = Game(27, argentina.reference, iran.reference, june(21,13,00), beloHorizonte)
    val game28 = Game(28, nigeria.reference, bosniaAndHerzegovina.reference, june(21,19,00), cuiaba)
    val game29 = Game(29, germany.reference, ghana.reference, june(21,16,00), fortaleza)
    val game30 = Game(30, unitedStates.reference, portugal.reference, june(22,19,00), manaus)
    val game31 = Game(31, belgium.reference, russia.reference, june(22,13,00), rioDeJaneiro)
    val game32 = Game(32, southKorea.reference, algeria.reference, june(22,16,00), portoAlegre)

    val game33 = Game(33, cameroon.reference, brazil.reference, june(23,17,00), brasilia)
    val game34 = Game(34, croatia.reference, mexico.reference, june(23,17,00), recife)
    val game35 = Game(35, australia.reference, spain.reference, june(23,13,00), curitiba)
    val game36 = Game(36, netherlands.reference, chile.reference, june(23,13,00), saoPaulo)
    val game37 = Game(37, japan.reference, colombia.reference, june(24,17,00), cuiaba)
    val game38 = Game(38, greece.reference, ivoryCoast.reference, june(24,17,00), fortaleza)
    val game39 = Game(39, italy.reference, uruguay.reference, june(24,13,00), natal)
    val game40 = Game(40, costaRica.reference, england.reference, june(24,13,00), beloHorizonte)
    val game41 = Game(41, honduras.reference, switzerland.reference, june(25,17,00), manaus)
    val game42 = Game(42, ecuador.reference, france.reference, june(25,17,00), rioDeJaneiro)
    val game43 = Game(43, nigeria.reference, argentina.reference, june(25,13,00), portoAlegre)
    val game44 = Game(44, bosniaAndHerzegovina.reference, iran.reference, june(25,13,00), salvador)
    val game45 = Game(45, unitedStates.reference, germany.reference, june(26,13,00), recife)
    val game46 = Game(46, portugal.reference, ghana.reference, june(26,13,00), brasilia)
    val game47 = Game(47, southKorea.reference, belgium.reference, june(26,17,00), saoPaulo)
    val game48 = Game(48, algeria.reference, russia.reference, june(26,17,00), curitiba)

    val groupA = WorldCupGroup("A", Seq(brazil, croatia, mexico, cameroon),
                               Seq(game01, game02, game17, game18, game33, game34))
    val groupB = WorldCupGroup("B", Seq(spain, netherlands, chile, australia),
                               Seq(game03, game04, game19, game20, game35, game36))
    val groupC = WorldCupGroup("C", Seq(colombia, greece, ivoryCoast, japan),
                               Seq(game05, game06, game21, game22, game37, game38))
    val groupD = WorldCupGroup("D", Seq(uruguay, costaRica, england, italy),
                               Seq(game07, game08, game23, game24, game39, game40))
    val groupE = WorldCupGroup("E", Seq(switzerland, ecuador, france, honduras),
                               Seq(game09, game10, game25, game26, game41, game42))
    val groupF = WorldCupGroup("F", Seq(argentina, bosniaAndHerzegovina, iran, nigeria),
                               Seq(game11, game12, game27, game28, game43, game44))
    val groupG = WorldCupGroup("G", Seq(germany, portugal, ghana, unitedStates),
                               Seq(game13, game14, game29, game30, game45, game46))
    val groupH = WorldCupGroup("H", Seq(belgium, algeria, russia, southKorea),
                               Seq(game15, game16, game31, game32, game47, game48))

    Group.init(Seq(groupA, groupB, groupC, groupD, groupE, groupF, groupG, groupH))

    val game49 = Game(49, GroupWinner(groupA), GroupRunnerUp(groupB), june(28,13,00), beloHorizonte)
    val game50 = Game(50, GroupWinner(groupC), GroupRunnerUp(groupD), june(28,17,00), rioDeJaneiro)
    val game51 = Game(51, GroupWinner(groupB), GroupRunnerUp(groupA), june(29,13,00), fortaleza)
    val game52 = Game(52, GroupWinner(groupD), GroupRunnerUp(groupC), june(29,17,00), recife)
    val game53 = Game(53, GroupWinner(groupE), GroupRunnerUp(groupF), june(30,13,00), brasilia)
    val game54 = Game(54, GroupWinner(groupG), GroupRunnerUp(groupH), june(30,17,00), portoAlegre)
    val game55 = Game(55, GroupWinner(groupF), GroupRunnerUp(groupE), july(1,13,00), saoPaulo)
    val game56 = Game(56, GroupWinner(groupH), GroupRunnerUp(groupG), july(1,17,00), salvador)

    val game57 = Game(57, GameWinner(game53), GameWinner(game54), july(4,13,00), rioDeJaneiro)
    val game58 = Game(58, GameWinner(game49), GameWinner(game50), july(4,17,00), fortaleza)
    val game59 = Game(59, GameWinner(game55), GameWinner(game56), july(5,13,00), brasilia)
    val game60 = Game(60, GameWinner(game51), GameWinner(game52), july(5,17,00), salvador)

    val game61 = Game(61, GameWinner(game57), GameWinner(game58), july(8,17,00), beloHorizonte)
    val game62 = Game(62, GameWinner(game59), GameWinner(game60), july(9,17,00), saoPaulo)

    val game63 = Game(63, GameLoser(game61), GameLoser(game62), july(12,17,00), brasilia)

    val game64 = Game(64, GameWinner(game61), GameWinner(game62), july(13,16,00), rioDeJaneiro)

    val matchDay1 = MatchDay("matchday1", "matchday_1",
                             Seq(game01, game02, game03, game04, game05, game06, game07, game08,
                                 game09, game10, game11, game12, game13, game14, game15, game16))
    val matchDay2 = MatchDay("matchday2", "matchday_2",
                             Seq(game17, game18, game19, game20, game21, game22, game23, game24,
                                 game25, game26, game27, game28, game29, game30, game31, game32))
    val matchDay3 = MatchDay("matchday3", "matchday_3",
                             Seq(game33, game34, game35, game36, game37, game38, game39, game40,
                                 game41, game42, game43, game44, game45, game46, game47, game48))
    val roundOf16 = MatchDay("round-of-16", "round_of_16",
                             Seq(game49, game50, game51, game52, game53, game54, game55, game56))
    val quarterFinals = MatchDay("quarter-finals", "quarter_finals", Seq(game57, game58, game59, game60))
    val semiFinals = MatchDay("semi-finals", "semi_finals", Seq(game61, game62))
    val matchDayThirdPlace = MatchDay("third-place-match", "third_place_match", Seq(game63))
    val matchDayFinal = MatchDay("final", "final", Seq(game64))

    MatchDay.init(Seq(matchDay1, matchDay2, matchDay3,
                      roundOf16, quarterFinals, semiFinals, matchDayThirdPlace, matchDayFinal))
  }
}
