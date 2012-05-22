package de.dimond.tippspiel.model

import net.liftweb.common._
import org.scala_tools.time.Imports._

import net.liftweb.http.S._

import PersistanceConfiguration._

object Trivia {
  def generate(game: Game, user: User, goalsHome: Int, goalsAway: Int): Option[String] = {
    val tips = Tip.forUsersAndGame(user.friends, game).values.toSeq

    val all: Seq[String] = {
      numberOfFriends(user, tips) ++
      (if (user.friends.size >= 3) {
        onlyTip(tips, goalsHome, goalsAway) ++
        winner(game, tips) ++
        unusualTips(game, tips, goalsHome, goalsAway)
      } else {
        Seq()
      }) ++
      (TipStatsUpdater.statsForGame(game) match {
        case Some(stats) => {
          twiceTheAverage(goalsHome, stats.averageGoalsHome, game.teamHome) ++
          twiceTheAverage(goalsAway, stats.averageGoalsAway, game.teamAway) ++
          averageMargin(stats, game) ++
          commonTip(goalsHome, goalsAway, stats) ++
          moreOrLessGoals(goalsHome, stats, game.teamHome, home = true) ++
          moreOrLessGoals(goalsAway, stats, game.teamAway, home = false)
        }
        case None => Seq()
      })
    }

    if (all.size > 0) {
      Some(all(util.Random.nextInt(all.size)))
    } else {
      None
    }
  }

  private def twiceTheAverage(goalsTipped: Int, goalsAverage: Double, team: TeamReference): Seq[String] = {
    if (goalsTipped > 2*goalsAverage) {
      Seq(?("trivia_twice_the_average").format(team.asString))
    } else {
      Seq()
    }
  }

  private def averageMargin(stats: TipStats, game: Game): Seq[String] = {
    if (math.abs(stats.averageGoalsAway - stats.averageGoalsHome) > 1) {
      val diff = math.abs(stats.averageGoalsAway - stats.averageGoalsHome).toInt
      val teamString = if (stats.averageGoalsAway > stats.averageGoalsHome) {
        game.teamAway.asString
      } else {
        game.teamHome.asString
      }
      Seq(?("trivia_average_margin").format(teamString, diff))
    } else {
      Seq()
    }
  }

  private def commonTip(goalsHome: Int, goalsAway: Int, stats: TipStats): Seq[String] = {
    val numYourTip = stats.numberOfTipsWhere(Some(goalsHome), Some(goalsAway))
    val total = stats.numberOfTipsWhere()
    val percent = numYourTip*100/total;
    if (percent > 10) {
      Seq(?("trivia_common_tip").format(percent))
    } else {
      Seq()
    }
  }

  private def moreOrLessGoals(goals: Int, stats: TipStats, team: TeamReference, home: Boolean = true): Seq[String] = {
    val tipsMore = {
      for (i <- (goals + 1) to 9) yield {
        if (home) {
          stats.numberOfTipsWhere(goalsHome = Some(i))
        } else {
          stats.numberOfTipsWhere(goalsAway = Some(i))
        }
      }
    }.sum
    val tipsLess = {
      for (i <- 0 until goals) yield {
        if (home) {
          stats.numberOfTipsWhere(goalsHome = Some(i))
        } else {
          stats.numberOfTipsWhere(goalsAway = Some(i))
        }
      }
    }.sum
    val totalTips = stats.numberOfTipsWhere()
    if (tipsMore > tipsLess) {
      val percent = tipsMore*100/totalTips
      Seq(?("trivia_more_goals").format(percent, team.asString))
    } else {
      val percent = tipsLess*100/totalTips
      Seq(?("trivia_less_goals").format(percent, team.asString))
    }
  }

  private def numberOfFriends(user: User, tips: Seq[Tip]): Seq[String] = {
    if (tips.size == 0) {
      Seq(?("trivia_first_friend"))
    } else {
      Seq(?("trivia_amount_of_friends").format(tips.size, user.friends.size))
    }
  }

  private def onlyTip(tips: Seq[Tip], gh: Int, ga: Int): Seq[String] = {
    if (tips.filter(t => t.goalsHome == gh && t.goalsAway == ga).size == 0) {
      Seq(?("trivia_only_tip_friends"))
    } else {
      Seq()
    }
  }

  private def winner(game: Game, tips: Seq[Tip]): Seq[String] = {
    val winsHome = tips.filter(t => t.goalsHome > t.goalsAway).size
    val winsAway = tips.filter(t => t.goalsAway > t.goalsHome).size
    if (1.0*(winsHome + winsAway)/tips.size < 0.5) {
      Seq(?("trivia_most_friends_draw"))
    } else if (math.abs(winsHome - winsAway)/tips.size < 0.25) {
      Seq("Your friends are split over who will win this match")
      Seq(?("trivia_most_friends_undecided"))
    } else if (winsHome > 2*winsAway) {
      Seq(?("trivia_most_friends_team_x").format(game.teamHome.asString))
    } else if (winsAway > 2*winsHome) {
      Seq(?("trivia_most_friends_team_x").format(game.teamAway.asString))
    } else {
      Seq()
    }
  }

  private def unusualTips(game: Game, tips: Seq[Tip], gh: Int, ga: Int): Seq[String] = {
    val moreHomeGoals = tips.filter(_.goalsHome > gh).size
    val moreAwayGoals = tips.filter(_.goalsAway > ga).size
    val moreTotalGoals = tips.filter(t => t.goalsHome + t.goalsAway > gh + ga)
    if (moreTotalGoals == 0) {
      Seq(?("trivia_more_goals_friends"))
    } else if (moreHomeGoals == 0) {
      Seq(?("trivia_not_more_goals").format(game.teamHome.asString, gh))
    } else if (moreAwayGoals == 0) {
      Seq(?("trivia_not_more_goals").format(game.teamAway.asString, ga))
    } else {
      Seq()
    }
  }
}

