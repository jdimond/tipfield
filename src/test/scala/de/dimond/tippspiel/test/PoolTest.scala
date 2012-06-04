package de.dimond.tippspiel.test

import net.liftweb.http.{S, LiftSession}
import net.liftweb.util._
import net.liftweb.common._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._

class PoolTest extends ModelSpec {

  val defaultUser = User.create("Test User 1", "10031")
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

  "Pool" should "not add user if there is no invitation for private pools" in {
    val poolBox = Pool.newPool("Name", "Description", false, defaultUser)
    val pool = poolBox.open_!
    val newUser = User.create("Test User 2", "30012")
    newUser.save()
    pool.addUser(newUser) should be (false)
  }

  "Pool" should "add user if there is no invitation for public pools" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    val newUser = User.create("Test User 2", "30013")
    newUser.save()
    pool.addUser(newUser) should be (true)
    pool.users should be (Set(newUser.id, defaultUser.id))
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
    pool.inviteUser("10030", None) should be (true)
    val user = User.create("Test User 1", "10030")
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

  "Invitations" should "work from one user for multiple recepients" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    val user = User.create("Test User 1", "80031")
    user.save() should be (true)
    pool.inviteUser("80032", Some(user))
    pool.inviteUser("80033", Some(user))
    pool.inviteUser("80034", Some(user))
    pool.inviteUser("80035", Some(user))
    pool.userIsInvited("80032") should be (true)
    pool.userIsInvited("80033") should be (true)
    pool.userIsInvited("80034") should be (true)
    pool.userIsInvited("80035") should be (true)
  }

  "Invitations" should "disappear after user joined and left pool" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    val user = User.create("Test User 1", "90031")
    user.save() should be (true)
    pool.inviteUser(user.fbId, Some(defaultUser))
    pool.addUser(user)
    Pool.invitationsForUser(user) should be (Set())
    pool.removeUser(user)
    Pool.invitationsForUser(user) should be (Set())
    Pool.invitationsForUser(user, true) should be (Set(pool))
  }

  "Invitations" should "still work after user left pool" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    val user = User.create("Test User 1", "100031")
    user.save() should be (true)
    pool.inviteUser(user.fbId, Some(defaultUser))
    pool.addUser(user)
    Pool.invitationsForUser(user) should be (Set())
    pool.removeUser(user)
    pool.inviteUser(user.fbId, Some(defaultUser))
    pool.userIsInvited(user.fbId) should be (true)
    Pool.invitationsForUser(user) should be (Set(pool))
  }

  "Invitation Link" should "be generated for Users" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    val invitationLinkBox = pool.invitationLinkForUser(defaultUser)
    invitationLinkBox.isEmpty should be (false)
    val invitationLink = invitationLinkBox.open_!
    invitationLink.userId should be (defaultUser.id)
    invitationLink.poolId should be (pool.id)
    invitationLink.invitationId.length should be > (10)
  }

  "Invitation Link" should "be unique for different users" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    val newUser = User.create("Test User 2", "110012")
    newUser.save() should be (true)
    pool.addUser(newUser)
    val il1Box = pool.invitationLinkForUser(defaultUser)
    val il2Box = pool.invitationLinkForUser(newUser)
    il1Box.isEmpty should be (false)
    il2Box.isEmpty should be (false)
    val il1 = il1Box.open_!
    il1.userId should be (defaultUser.id)
    il1.poolId should be (pool.id)
    val il2 = il2Box.open_!
    il2.userId should be (newUser.id)
    il2.poolId should be (pool.id)
    il1.invitationId should not be (il2.invitationId)
  }

  "Invitation Links" should "not exist for non-admins in private pools" in {
    val poolBox = Pool.newPool("Name", "Description", false, defaultUser)
    val pool = poolBox.open_!
    val newUser = User.create("Test User 2", "130012")
    newUser.save() should be (true)
    pool.addUser(newUser)
    pool.invitationLinkForUser(newUser) should be (Empty)
  }

  "Invitation Links" should "not exist for users not in the group" in {
    val poolBox = Pool.newPool("Name", "Description", true, defaultUser)
    val pool = poolBox.open_!
    val newUser = User.create("Test User 2", "120012")
    newUser.save() should be (true)
    pool.invitationLinkForUser(newUser) should be (Empty)
  }

  "Invitation Links" should "should be unique for different pools" in {
    val poolBox1 = Pool.newPool("Name", "Description", true, defaultUser)
    val poolBox2 = Pool.newPool("Name", "Description", true, defaultUser)
    val pool1 = poolBox1.open_!
    val pool2 = poolBox2.open_!

    val il1Box = pool1.invitationLinkForUser(defaultUser)
    val il2Box = pool2.invitationLinkForUser(defaultUser)
    il1Box.isEmpty should be (false)
    il2Box.isEmpty should be (false)
    val il1 = il1Box.open_!
    il1.userId should be (defaultUser.id)
    il1.poolId should be (pool1.id)
    val il2 = il2Box.open_!
    il2.userId should be (defaultUser.id)
    il2.poolId should be (pool2.id)
    il1.invitationId should not be (il2.invitationId)
  }
}
