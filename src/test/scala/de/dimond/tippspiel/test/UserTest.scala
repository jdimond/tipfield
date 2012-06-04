package de.dimond.tippspiel.test

import net.liftweb.http.{S, LiftSession}
import net.liftweb.util._
import net.liftweb.common._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import org.scala_tools.time.Imports._

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._

class UserTest extends ModelSpec {

  "User" should "be saved" in {
    val user1 = User.create("Test User 1", "10000001")
    user1.save() should be (true)
  }

  "User" should "not allowed to be saved twice with the same FB ID" in {
    val user1 = User.create("Test User 1", "10000002")
    user1.save() should be (true)
    val user2 = User.create("Test User 1", "10000002")
    user2.save() should be (false)
  }
}
