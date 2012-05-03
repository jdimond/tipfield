package de.dimond.tippspiel.test

import net.liftweb.http.{S, LiftSession}
import net.liftweb.util._
import net.liftweb.common._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import de.dimond.tippspiel.model._

object Lock

class ModelSpec extends FlatSpec with ShouldMatchers {

  val session : LiftSession = new LiftSession("", StringHelpers.randomString(20), Empty)

  Lock.synchronized {
    if (!PersistanceConfiguration.initialized) {
      PersistanceConfiguration.initialize()
      PersistanceConfiguration.flush_!()
    }
  }

  override def withFixture(test: NoArgTest) {
    try {
      S.initIfUninitted(session) {
        test()
      }
    } finally {
    }
  }
}
