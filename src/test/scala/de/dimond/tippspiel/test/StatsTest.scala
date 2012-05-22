package de.dimond.tippspiel.test

import net.liftweb.http.{S, LiftSession}
import net.liftweb.util._
import net.liftweb.common._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import org.scala_tools.time.Imports._

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._

class StatsTest extends ModelSpec {

  val teamOne = Team("Team One", "", 1000)
  val teamTwo = Team("Team Two", "", 2000)

  "Stats" should "be correct" in {
    val user1 = User.create("Test User 1", "1100001")
    user1.save() should be (true)
    val user2 = User.create("Test User 2", "1100002")
    user2.save() should be (true)
    val user3 = User.create("Test User 3", "1100003")
    user3.save() should be (true)
    val user4 = User.create("Test User 4", "1100004")
    user4.save() should be (true)
    val user5 = User.create("Test User 5", "1100005")
    user5.save() should be (true)

    val gameOne = Game(10000, teamOne.reference, teamTwo.reference, DateTime.now + (1 hour), Location("Here"))

    Tip.saveForUserAndGame(user1, gameOne, 1, 0) should be (true)
    Tip.saveForUserAndGame(user2, gameOne, 1, 0) should be (true)
    Tip.saveForUserAndGame(user3, gameOne, 3, 2) should be (true)
    Tip.saveForUserAndGame(user4, gameOne, 1, 2) should be (true)
    Tip.saveForUserAndGame(user5, gameOne, 4, 4) should be (true)

    val statsOption = Tip.statsForGame(gameOne)
    statsOption should not be ('empty)
    val stats = statsOption.get
    stats.numberOfTipsWhere() should be (5)
    stats.numberOfTipsWhere(goalsHome = Some(0)) should be (0)
    stats.numberOfTipsWhere(goalsHome = Some(1)) should be (3)
    stats.numberOfTipsWhere(goalsHome = Some(2)) should be (0)
    stats.numberOfTipsWhere(goalsHome = Some(3)) should be (1)
    stats.numberOfTipsWhere(goalsHome = Some(4)) should be (1)

    stats.numberOfTipsWhere(goalsAway = Some(0)) should be (2)
    stats.numberOfTipsWhere(goalsAway = Some(1)) should be (0)
    stats.numberOfTipsWhere(goalsAway = Some(2)) should be (2)
    stats.numberOfTipsWhere(goalsAway = Some(3)) should be (0)
    stats.numberOfTipsWhere(goalsAway = Some(4)) should be (1)

    stats.numberOfTipsWhere(goalsHome = Some(0), goalsAway = Some(0)) should be (0)
    stats.numberOfTipsWhere(goalsHome = Some(1), goalsAway = Some(0)) should be (2)
    stats.numberOfTipsWhere(goalsHome = Some(3), goalsAway = Some(2)) should be (1)
    stats.numberOfTipsWhere(goalsHome = Some(1), goalsAway = Some(2)) should be (1)
    stats.numberOfTipsWhere(goalsHome = Some(4), goalsAway = Some(4)) should be (1)
    stats.numberOfTipsWhere(goalsHome = Some(3), goalsAway = Some(4)) should be (0)

    stats.averageGoalsHome should be (2.0)
    stats.averageGoalsAway should be (1.6)
    stats.numberOfDraws should be (1)
    stats.numberOfHomeWins should be (3)
    stats.numberOfAwayWins should be (1)
  }

  "Stats" should "work for multiple games" in {
    val user1 = User.create("Test User 1", "1100011")
    user1.save() should be (true)
    val user2 = User.create("Test User 2", "1100012")
    user2.save() should be (true)
    val user3 = User.create("Test User 3", "1100013")
    user3.save() should be (true)

    val gameOne = Game(10001, teamOne.reference, teamTwo.reference, DateTime.now + (1 hour), Location("Here"))
    val gameTwo = Game(10002, teamOne.reference, teamTwo.reference, DateTime.now + (1 hour), Location("Here"))

    Tip.saveForUserAndGame(user1, gameOne, 1, 0) should be (true)
    Tip.saveForUserAndGame(user2, gameOne, 1, 0) should be (true)
    Tip.saveForUserAndGame(user3, gameOne, 1, 2) should be (true)
    Tip.saveForUserAndGame(user1, gameTwo, 1, 0) should be (true)
    Tip.saveForUserAndGame(user2, gameTwo, 3, 3) should be (true)

    val statsOption = Tip.statsForGame(gameOne)
    statsOption should not be ('empty)
    val stats = statsOption.get
    stats.numberOfTipsWhere() should be (3)
    stats.numberOfTipsWhere(goalsHome = Some(0)) should be (0)
    stats.numberOfTipsWhere(goalsHome = Some(1)) should be (3)
    stats.numberOfTipsWhere(goalsHome = Some(2)) should be (0)
    stats.numberOfTipsWhere(goalsAway = Some(0)) should be (2)
    stats.numberOfTipsWhere(goalsAway = Some(1)) should be (0)
    stats.numberOfTipsWhere(goalsAway = Some(2)) should be (1)

    stats.averageGoalsHome should be (1)
    stats.averageGoalsAway should be (2.0/3)

    val statsOption2 = Tip.statsForGame(gameTwo)
    statsOption2 should not be ('empty)
    val stats2 = statsOption2.get
    stats2.numberOfTipsWhere() should be (2)
  }
}
