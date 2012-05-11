package de.dimond.tippspiel.test

import net.liftweb.http.{S, LiftSession}
import net.liftweb.util._
import net.liftweb.common._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._

class FacebookRequestTest extends ModelSpec {

  val defaultUser = User.create("Test User 1", "123000")
  defaultUser.save()

  "Facebook Requests" should "be persistent" in {
    FacebookRequests.saveRequestForUser(defaultUser, "1", "12300", 0)
    FacebookRequests.getRequests("1").map(_.requestId) should be (List("12300"))
  }

  "Facebook Requests" should "be able to be deleted" in {
    FacebookRequests.saveRequestForUser(defaultUser, "2", "12301", 0)
    FacebookRequests.getRequests("2").map(_.requestId) should be (List("12301"))
    FacebookRequests.deleteRequest("2", "12301") should be (true)
    FacebookRequests.getRequests("2") should be (List())
  }

  "Facebook Requests" should "be able to be deleted alone" in {
    FacebookRequests.saveRequestForUser(defaultUser, "3", "12302", 0)
    FacebookRequests.saveRequestForUser(defaultUser, "3", "12303", 0)
    FacebookRequests.getRequests("3").map(_.requestId).sorted should be (List("12302", "12303"))
    FacebookRequests.deleteRequest("3", "12302") should be (true)
    FacebookRequests.getRequests("3").map(_.requestId) should be (List("12303"))
  }

  "Facebook Requests" should "be able to be deleted in bulks" in {
    FacebookRequests.saveRequestForUser(defaultUser, "4", "12304", 0)
    FacebookRequests.saveRequestForUser(defaultUser, "4", "12305", 0)
    FacebookRequests.getRequests("4").map(_.requestId).sorted should be (List("12304", "12305"))
    FacebookRequests.deleteAllRequests("4") should be (true)
    FacebookRequests.getRequests("4") should be (List())
  }

  "Facebook Requests" should "work with multiple users and requests" in {
    val newUser = User.create("Test User 2", "1230001")
    newUser.save()

    FacebookRequests.saveRequestForUser(defaultUser, "5", "12306", 0)
    FacebookRequests.saveRequestForUser(newUser, "5", "12307", 0)
    FacebookRequests.saveRequestForUser(defaultUser, "6", "12308", 0)
    FacebookRequests.saveRequestForUser(newUser, "6", "12309", 0)
    FacebookRequests.getRequests("5").map(_.requestId).sorted should be (List("12306", "12307"))
    FacebookRequests.getRequests("6").map(_.requestId).sorted should be (List("12308", "12309"))

    FacebookRequests.deleteAllRequests("5") should be (true)
    FacebookRequests.getRequests("5") should be (List())
    FacebookRequests.getRequests("6").map(_.requestId).sorted should be (List("12308", "12309"))

    FacebookRequests.saveRequestForUser(newUser, "5", "12310", 0)
    FacebookRequests.saveRequestForUser(newUser, "5", "12311", 0)
    FacebookRequests.getRequests("5").map(_.requestId).sorted should be (List("12310", "12311"))

    FacebookRequests.deleteRequest("6", "12308") should be (true)
    FacebookRequests.getRequests("5").map(_.requestId).sorted should be (List("12310", "12311"))
    FacebookRequests.getRequests("6").map(_.requestId).sorted should be (List("12309"))
  }

  "Facebook Requests" should "work with different pools" in {
    FacebookRequests.saveRequestForUser(defaultUser, "7", "12312", 0)
    FacebookRequests.saveRequestForUser(defaultUser, "7", "12313", 1)
    FacebookRequests.getRequests("7").map(_.requestId).sorted should be (List("12312", "12313"))
    FacebookRequests.getRequests("7", 0).map(_.requestId).sorted should be (List("12312"))
    FacebookRequests.getRequests("7", 1).map(_.requestId).sorted should be (List("12313"))
  }
}
