package de.dimond.tippspiel.test

import net.liftweb.http.{S, LiftSession}
import net.liftweb.util._
import net.liftweb.common._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import org.scala_tools.time.Imports._

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._

class TipsTest extends ModelSpec {

  GameData.init()
  SpecialData.init()

  "Tips" should "be saved" in {
    val user1 = User.create("Test User 1", "1000001")
    user1.save()
    val game1 = Game.all(0)
    Tip.saveForUserAndGame(user1, game1, 2, 1) should be (true)
    val tipBox = Tip.forUserAndGame(user1, game1)
    tipBox.isEmpty should be (false)
    val tip = tipBox.open_!
    tip.goalsHome should be (2)
    tip.goalsAway should be (1)
    (tip.submissionTime to DateTime.now).millis should be < (1000L)
  }

  "Tips" should "be found in map" in {
    val user1 = User.create("Test User 1", "1000002")
    user1.save()
    val game1 = Game.all(0)
    val game2 = Game.all(1)
    Tip.saveForUserAndGame(user1, game1, 2, 1) should be (true)
    Tip.saveForUserAndGame(user1, game2, 4, 3) should be (true)
    val map = Tip.forUserAndGames(user1, Seq(game1, game2))
    map should have size (2)
    Tip.forUserAndGames(user1, Game.all) should have size (2)
    map.get(game1).isEmpty should be (false)
    map.get(game2).isEmpty should be (false)
    map(game1).goalsHome should be (2)
    map(game1).goalsAway should be (1)
    map(game2).goalsHome should be (4)
    map(game2).goalsAway should be (3)
  }

  "Tips" should "work for multiple users" in {
    val user1 = User.create("Test User 1", "1000003")
    user1.save()
    val user2 = User.create("Test User 2", "1000004")
    user2.save()
    val game1 = Game.all(0)
    val game2 = Game.all(1)
    Tip.saveForUserAndGame(user1, game1, 2, 1) should be (true)
    Tip.saveForUserAndGame(user1, game2, 1, 2) should be (true)
    Tip.saveForUserAndGame(user2, game2, 1, 2) should be (true)
    val map = Tip.forUserAndGames(user1, Seq(game1, game2))
    map should have size (2)
    Tip.forUserAndGames(user2, Seq(game1, game2)) should have size (1)
  }

  "SpecialTips" should "be saved" in {
    val user1 = User.create("Test User", "1000005")
    user1.save() should be (true)

    val special1 = Special.all(0)
    SpecialTip.saveForUser(user1, special1, 1) should be (true)
    val answerBox = SpecialTip.answerForUser(user1, special1)
    answerBox.isEmpty should be (false)
    val answer = answerBox.open_!
    answer.userId should be (user1.id)
    answer.special should be (special1)
    answer.answerId should be (1)
    (answer.submissionTime to DateTime.now).millis should be < (1000L)
  }

  "SpecialTips" should "be found in map" in {
    val user1 = User.create("Test User", "1000006")
    user1.save() should be (true)

    val special1 = Special.all(0)
    val special2 = Special.all(1)
    SpecialTip.saveForUser(user1, special1, 2) should be (true)
    SpecialTip.saveForUser(user1, special2, 3) should be (true)
    val answers = SpecialTip.answersForUser(user1, Seq(special1, special2))
    answers should have size (2)
    answers(special1).answerId should be (2)
    answers(special2).answerId should be (3)
    SpecialTip.answersForUser(user1, Special.all) should have size (2)
  }

  "SpecialTips" should "work for multiple users" in {
    val user1  = User.create("Test User 1", "1000007")
    user1.save() should be (true)
    val user2  = User.create("Test User 2", "1000008")
    user2.save() should be (true)

    val special1 = Special.all(0)
    val special2 = Special.all(1)
    SpecialTip.saveForUser(user1, special1, 4) should be (true)
    SpecialTip.saveForUser(user1, special2, 5) should be (true)
    SpecialTip.saveForUser(user2, special2, 6) should be (true)
    val answers1 = SpecialTip.answersForUser(user1, Seq(special1, special2))
    val answers2 = SpecialTip.answersForUser(user2, Seq(special1, special2))
    answers1 should have size (2)
    answers2 should have size (1)
    answers1(special1).answerId should be (4)
    answers1(special2).answerId should be (5)
    answers2(special2).answerId should be (6)
  }
}
