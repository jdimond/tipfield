package de.dimond.tippspiel.test

import net.liftweb.http.{S, LiftSession}
import net.liftweb.util._
import net.liftweb.common._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import de.dimond.tippspiel.lib._

class FacebookApiTests extends FlatSpec with ShouldMatchers {
  val request = "ZcZocIFknCpcTLhwsRwwH5nL6oq7OmKWJx41xRTi59E.eyJhbGdvcml0aG0iOiJITUFDLVNIQTI1NiIsImV4cGlyZX" +
                "MiOiIxMjczMzU5NjAwIiwib2F1dGhfdG9rZW4iOiIyNTQ3NTIwNzMxNTJ8Mi5JX2VURmtjVEtTelg1bm8zakk0cjFR" +
                "X18uMzYwMC4xMjczMzU5NjAwLTE2Nzc4NDYzODV8dUk3R3dybUJVZWQ4c2VaWjA1SmJkekdGVXBrLiIsInNlc3Npb2" +
                "5fa2V5IjoiMi5JX2VURmtjVEtTelg1bm8zakk0cjFRX18uMzYwMC4xMjczMzU5NjAwLTE2Nzc4NDYzODUiLCJ1c2Vy" +
                "X2lkIjoiMTY3Nzg0NjM4NSJ9"
  val secret = "904270b68a2cc3d54485323652da4d14"

  "Facebook signed requests" should "work" in {
    val api = new FacebookApi(secret)
    api.parseSignedRequest(request) should not be (None)
  }
}
