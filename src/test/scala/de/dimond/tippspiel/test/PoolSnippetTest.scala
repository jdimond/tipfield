package de.dimond.tippspiel.test

import net.liftweb.http.{S, LiftSession}
import net.liftweb.util._
import net.liftweb.common._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._
import de.dimond.tippspiel.snippet._

class PoolSnippetTest extends ModelSpec {

  "Pool Snippet" should "handle callback properly" in {
    val user1 = User.create("Test User", "10041")
    user1.save()
    User.logUserIn(user1)
    val pool = Pool.newPool("Test Pool", "Description", true, user1).open_!

    val poolSnippetExtended = new Pools() {
      override val currentPool = pool
    }

    val callbackResponse = Map("request" -> "123456", "to" -> List("10042", "10043", "10044"))

    poolSnippetExtended.ajaxFriendsButtonResponseHandler(callbackResponse)

    pool.userIsInvited("10042") should be (true)
    pool.userIsInvited("10043") should be (true)
    pool.userIsInvited("10044") should be (true)
  }

}
