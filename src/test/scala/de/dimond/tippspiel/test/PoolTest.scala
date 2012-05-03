package de.dimond.tippspiel.test

import net.liftweb.http.{S, LiftSession}
import net.liftweb.util._
import net.liftweb.common._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._

class PoolTest extends ModelSpec {

  val defaultUser = User.create("Test User 1", "10011")
  defaultUser.save()

  "Pool" should "be created" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    poolBox.isEmpty should be (false)
    val pool = poolBox.open_!
    (pool.id.toInt) should be > (0)
    pool.name should be ("Name")
    pool.description should be ("Description")
    pool.users should be (Set(defaultUser.id))
    pool.adminId should be (defaultUser.id)
  }

  "Pool" should "add users" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    val newUser = User.create("Test User 2", "10012")
    newUser.save()
    pool.inviteUser("10012", None) should be (true)
    pool.addUser(newUser) should be (true)
    pool.users should be (Set(defaultUser.id, newUser.id))
  }

  "Pool" should "remove users" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    val newUser = User.create("Test User 2", "20012")
    newUser.save()
    pool.inviteUser("20012", None) should be (true)
    pool.addUser(newUser) should be (true)
    pool.removeUser(newUser) should be (true)
    pool.users should be (Set(defaultUser.id))
  }

  "Pool" should "not add user if there is no invitation" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    val newUser = User.create("Test User 2", "30012")
    newUser.save()
    pool.addUser(newUser) should be (false)
  }

  "Pool" should "remove admin" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    pool.removeUser(defaultUser) should be (true)
    pool.users should be (Set())
  }

  "Pool" should "indicate user has left group" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    val newUser = User.create("Test User 2", "40012")
    newUser.save()
    pool.inviteUser("40012", None) should be (true)
    pool.userHasLeftGroup(newUser.id) should be (Empty)
    pool.addUser(newUser) should be (true)
    pool.userHasLeftGroup(newUser.id) should be (Full(false))
    pool.removeUser(newUser) should be (true)
    pool.userHasLeftGroup(newUser.id) should be (Full(true))
  }

  "Pool" should "indicate invitation" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    pool.inviteUser("123456", None) should be (true)
    pool.userIsInvited("123456") should be (true)
    pool.userIsInvited("1234567") should be (false)
  }

  "Invitations" should "appear in invitation list" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    pool.inviteUser("10031", None) should be (true)
    val user = User.create("Test User 1", "10031")
    user.save() should be (true)
    Pool.invitationsForUser(user) should be (Set(pool))
  }

  "Invitations" should "should disappear after addition to pool" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    pool.inviteUser("20031", None) should be (true)
    val user = User.create("Test User 1", "20031")
    user.save() should be (true)
    pool.addUser(user)
    Pool.invitationsForUser(user) should be (Set())
    Pool.invitationsForUser(user, true, true) should be (Set(pool))
  }

  "Invitations" should "be able to be ignored" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    pool.inviteUser("30031", None) should be (true)
    val user = User.create("Test User 1", "30031")
    user.save() should be (true)
    pool.ignoreInvitations(user)
    Pool.invitationsForUser(user) should be (Set())
    Pool.invitationsForUser(user, true) should be (Set(pool))
  }

  "Invitations" should "work over multiple pools" in {
    val poolBox1 = Pool.newPool("Pool 1", "Description", true, defaultUser)
    val pool1 = poolBox1.open_!
    val poolBox2 = Pool.newPool("Pool 2", "Description", true, defaultUser)
    val pool2 = poolBox2.open_!
    pool1.inviteUser("40031", None) should be (true)
    pool2.inviteUser("40031", None) should be (true)

    val user = User.create("Test User 1", "40031")
    user.save() should be (true)
    pool1.ignoreInvitations(user)
    Pool.invitationsForUser(user) should be (Set(pool2))
    Pool.invitationsForUser(user, true).toSet should be (Set(pool1, pool2))
  }

  "Invitations" should "be able to be reactivated" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    pool.inviteUser("50031", None) should be (true)
    val user = User.create("Test User 1", "50031")
    user.save() should be (true)
    pool.ignoreInvitations(user)
    Pool.invitationsForUser(user) should be (Set())

    pool.inviteUser("50031", None) should be (true)
    Pool.invitationsForUser(user) should be (Set(pool))

    pool.ignoreInvitations(user)
    Pool.invitationsForUser(user) should be (Set())

    pool.inviteUser("50031", Some(defaultUser))
    Pool.invitationsForUser(user) should be (Set(pool))
  }

  "Invitations" should "be ignored from multiple people" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    pool.inviteUser("60031", None) should be (true)
    pool.inviteUser("60031", Some(defaultUser)) should be (true)
    val user = User.create("Test User 1", "60031")
    user.save() should be (true)
    pool.ignoreInvitations(user)
    Pool.invitationsForUser(user) should be (Set())
  }

  "Invitations" should "work after user joined" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    val user = User.create("Test User 1", "70031")
    user.save() should be (true)
    pool.inviteUser("70031", None) should be (true)
    Pool.invitationsForUser(user) should be (Set(pool))
  }
}
