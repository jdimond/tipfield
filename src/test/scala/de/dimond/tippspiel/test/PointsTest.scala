package de.dimond.tippspiel.test

import net.liftweb.http.{S, LiftSession}
import net.liftweb.util._
import net.liftweb.common._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import org.scala_tools.time.Imports._

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._

class PointsTest extends ModelSpec {

  GameData.init()
  SpecialData.init()

  def checkPoints(toCheck: Seq[(User, Int)]) {
    val check1 = toCheck.filter({case (u, i) => u.points != i})
    //check1 should be (Seq())
    val check2 = toCheck.map({case (u, i) => (User.findById(u.id).open_!, i)}).filter({case (u, i) => u.points != i})
    check2 should be (Seq())
  }

  "Points" should "work" in {
    val user1 = User.create("Test User 1", "2000001")
    user1.save() should be (true)
    val user2 = User.create("Test User 2", "2000002")
    user2.save() should be (true)
    val user3 = User.create("Test User 3", "2000003")
    user3.save() should be (true)

    val game1 = Game.all(0)
    val game2 = Game.all(1)
    val game3 = Game.all(2)
    val game4 = Game.all(3)
    val special1 = Special.all(0)
    val special2 = Special.all(1)

    Tip.saveForUserAndGame(user1, game1, 0, 0) should be (true)
    Tip.saveForUserAndGame(user1, game2, 1, 0) should be (true)
    Tip.saveForUserAndGame(user1, game3, 0, 1) should be (true)
    Tip.saveForUserAndGame(user1, game4, 1, 1) should be (true)
    SpecialTip.saveForUser(user1, special1, 0) should be (true)
    SpecialTip.saveForUser(user1, special2, 1) should be (true)

    Tip.saveForUserAndGame(user2, game1, 2, 1) should be (true)
    Tip.saveForUserAndGame(user2, game2, 0, 1) should be (true)
    Tip.saveForUserAndGame(user2, game3, 0, 1) should be (true)
    Tip.saveForUserAndGame(user2, game4, 2, 1) should be (true)
    SpecialTip.saveForUser(user2, special1, 1) should be (true)
    SpecialTip.saveForUser(user2, special2, 1) should be (true)

    Tip.saveForUserAndGame(user3, game1, 2, 0) should be (true)
    Tip.saveForUserAndGame(user3, game2, 1, 1) should be (true)
    Tip.saveForUserAndGame(user3, game3, 3, 0) should be (true)
    SpecialTip.saveForUser(user3, special1, 2) should be (true)

    Result.saveForGame(game1, 1, 0).isEmpty should be (false)
    checkPoints(Seq((user1, 0), (user2, 2), (user3, 1)))

    Result.saveForGame(game2, 0, 1).isEmpty should be (false)
    checkPoints(Seq((user1, 0), (user2, 5), (user3, 1)))

    Result.saveForGame(game2, 1, 1).isEmpty should be (false)
    checkPoints(Seq((user1, 0), (user2, 2), (user3, 4)))

    val s1p = special1.points
    val s2p = special2.points

    SpecialResult.save(special1, Some(0)).isEmpty should be (false)
    checkPoints(Seq((user1, s1p), (user2, 2), (user3, 4)))

    Result.saveForGame(game3, 0, 1).isEmpty should be (false)
    checkPoints(Seq((user1, s1p + 3), (user2, 5), (user3, 4)))

    SpecialResult.save(special1, None).isEmpty should be (false)
    checkPoints(Seq((user1, 3), (user2, 5), (user3, 4)))

    Result.saveForGame(game4, 1, 0).isEmpty should be (false)
    checkPoints(Seq((user1, 3), (user2, 7), (user3, 4)))

    SpecialResult.save(special1, Some(2)).isEmpty should be (false)
    checkPoints(Seq((user1, 3), (user2, 7), (user3, 4 + s1p)))

    SpecialResult.save(special2, Some(1)).isEmpty should be (false)
    checkPoints(Seq((user1, 3 + s2p), (user2, 7 + s2p), (user3, 4 + s1p)))
  }

}
