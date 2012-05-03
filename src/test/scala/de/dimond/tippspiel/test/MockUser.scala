package de.dimond.tippspiel.test

import de.dimond.tippspiel.model._

import org.joda.time.DateTime

class MockUser(val id: Long) extends User {
  var fullName = "Empty Mock User"
  var fbId = "1"

  def save() = true

  var isAdmin = false

  var firstName: Option[String] = None
  var middleName: Option[String] = None
  var lastName: Option[String] = None
  var gender: Option[String] = None
  var locale: Option[String] = None
  var fbUserName: Option[String] = None
  var fbAccessToken: Option[String] = None

  def setFbAccessToken(accessToken: Option[String], expiresAt: Option[DateTime]) = {
    fbAccessToken = accessToken
    fbAccessTokenExpires = expiresAt
  }
  var fbAccessTokenExpires: Option[DateTime] = None

  var fbTimeZone: Option[String] = None

  var facebookFriends: Set[String] = Set()
  var friends: Set[Long] = Set()
  var points = 0
  var ranking: Option[Int] = None

  override def toString = "MockUser[id=%d, name=%s, points=%s]".format(id, fullName, points)
}
