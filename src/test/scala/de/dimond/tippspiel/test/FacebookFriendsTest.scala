package de.dimond.tippspiel.test

import net.liftweb.http.{S, LiftSession}
import net.liftweb.util._
import net.liftweb.common._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._

class FacebookFriendsTest extends ModelSpec {

  "Facebook Friends" should "appear if they where added to a user" in {
    val user1 = User.create("Test User 1", "10001")
    user1.facebookFriends = Set("10002", "10003")
    user1.save() should be (true)
    val userFromDb = User.findByFbId("10001").open_!
    userFromDb.facebookFriends should be (Set("10002", "10003"))
  }

  "Facebook Friends" should "appear as friends if another user was created" in {
    val user1 = User.create("Test User 1", "20001")
    user1.facebookFriends = Set("20002", "20003")
    user1.save() should be (true)

    val user2 = User.create("Test User 2", "20002")
    user2.facebookFriends = Set("20001", "20003")
    user2.save() should be (true)

    val userFromDbBox = User.findByFbId("20001")
    val userFromDb = userFromDbBox.open_!
    userFromDb.friends should be (Set(user2.id))
  }

  "Facebook Friends" should "change on update" in {
    val user1 = User.create("Test User 1", "30001")
    user1.facebookFriends = Set("30002", "30001")
    user1.save() should be (true)

    user1.facebookFriends = Set("30002")
    user1.save()

    val userFromDb = User.findByFbId("30001").open_!
    userFromDb.facebookFriends should be (Set("30002"))
  }

  "Facebook Friends" should "appear in friends if the friend exists already" in {
    val user1 = User.create("Test User 1", "40001")
    user1.facebookFriends = Set("40002", "40003")
    user1.save() should be (true)

    val user2 = User.create("Test User 2", "40002")
    user2.facebookFriends = Set("40001", "40003")
    user2.save()

    val userFromDb = User.findByFbId("40002").open_!
    userFromDb.friends should be (Set(user1.id))
  }

  "Facebook Friends" should "disappear if the friendlist was updated" in {
    val user1 = User.create("Test User 1", "50001")
    user1.facebookFriends = Set("50002", "50003")
    user1.save() should be (true)

    val user2 = User.create("Test User 2", "50002")
    user2.facebookFriends = Set("50001", "50003")
    user2.save()

    user1.facebookFriends = Set("50003")
    user1.save()

    val userFromDb = User.findByFbId("50001").open_!
    userFromDb.friends should be (Set())
  }

  "Facebook Friends" should "appear if the friendlist was updated" in {
    val user1 = User.create("Test User 1", "60001")
    user1.facebookFriends = Set("60003")
    user1.save() should be (true)

    val user2 = User.create("Test User 2", "60002")
    user2.facebookFriends = Set("60001", "60003")
    user2.save()

    user1.facebookFriends = Set("60002", "60003")
    user1.save()

    val userFromDb = User.findByFbId("60001").open_!
    userFromDb.friends should be (Set(user2.id))
  }
}
