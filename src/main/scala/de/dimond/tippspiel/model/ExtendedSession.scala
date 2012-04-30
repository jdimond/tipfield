package de.dimond.tippspiel.model


import net.liftweb.common._
import net.liftweb.http.{S, Req}
import net.liftweb.http.provider.HTTPCookie
import org.scala_tools.time.Imports._

import PersistanceConfiguration._

import net.liftweb.common.Logger

trait ExtendedSession extends Logger {
  def CookieName = "ext_id"

  protected def saveUserAndGetCookieId(user: User, exp: DateTime): Box[String]

  protected def findUserIdForCookieId(cookieId: String): Box[(Long, DateTime)]

  protected def deleteCookieId(cookieId: String): Unit

  def expirationTime: DateTime = DateTime.now + 180.days

  def userDidLogin(user: User) {
    userDidLogout()
    val exp = expirationTime
    val idBox = saveUserAndGetCookieId(user, exp)
    idBox match {
      case Full(id) => {
        val cookie = HTTPCookie(CookieName, id).
        setMaxAge(((DateTime.now to exp).millis/1000L).toInt).
        setPath("/")
        S.addCookie(cookie)
      }
      case _ =>
    }
  }

  def userDidLogout() {
    for (cook <- S.findCookie(CookieName)) {
      S.deleteCookie(cook)
      cook.value.map { deleteCookieId(_) }
    }
  }

  def recoverUserId = (User.currentUser, User.currentUserId) match {
    case (Full(u), Full(id)) => Full(id)
    case _ => Empty
  }

  def testCookieEarlyInStateful: Box[Req] => Unit = {
    ignoredReq => {
      (User.currentUserId, S.findCookie(CookieName).map(_.value)) match {
        case (Empty, Full(Full(c))) => findUserIdForCookieId(c) match {
          case Full((_, exp)) if exp < DateTime.now => deleteCookieId(c)
          case Full((userId, exp)) => User.logUserIdIn(userId.toString)
          case _ =>
        }
        case _ =>
      }
    }
  }
}
