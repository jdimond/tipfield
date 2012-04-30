package de.dimond.tippspiel.model

import org.joda.time.DateTime
import org.scala_tools.time.Imports._

import net.liftweb.proto._
import net.liftweb.util.FieldError
import net.liftweb.common.{Box, Empty, Failure, Full}
import net.liftweb.common.Logger

import net.liftweb.http._
import js._
import JsCmds._
import scala.xml.{NodeSeq, Node, Text, Elem}
import scala.xml.transform._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util.Helpers._
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.util.Mailer._
import S._

import de.dimond.tippspiel.model.PersistanceConfiguration._

trait MetaUser[U <: User] extends ProtoUser with Logger {
  self: U =>

  type TheUserType = User

  protected implicit def typeToBridge(in: TheUserType): UserBridge =
    new MyUserBridge(in)

  def NOT_IMPL = new RuntimeException("NOT IMPL");

  protected class MyUserBridge(in: TheUserType) extends UserBridge {
    def userIdAsString: String = in.id.toString

    def getFirstName: String = throw NOT_IMPL;

    def getLastName: String = throw NOT_IMPL;

    def getEmail: String = throw NOT_IMPL;

    def superUser_? : Boolean = in.isAdmin;

    def validated_? : Boolean = throw NOT_IMPL;

    def testPassword(toTest: Box[String]): Boolean = throw NOT_IMPL;

    def setValidated(validation: Boolean): TheUserType = throw NOT_IMPL;

    def resetUniqueId(): TheUserType = throw NOT_IMPL;

    def getUniqueId(): String = throw NOT_IMPL;

    def validate: List[FieldError] = throw NOT_IMPL;

    def setPasswordFromListString(pwd: List[String]): TheUserType = throw NOT_IMPL;

    def save(): Boolean = throw NOT_IMPL;
  }

  override def signupFields = throw NOT_IMPL;
  override def editFields = throw NOT_IMPL;
  override def buildFieldBridge(from: FieldPointerType) = throw NOT_IMPL;
  override def findUserByUniqueId(id: String) = throw NOT_IMPL;
  override def findUserByUserName(username: String) = throw NOT_IMPL;
  override def computeFieldFromPointer(instance: TheUserType, pointer: FieldPointerType) = throw NOT_IMPL;
  override def createNewUserInstance() = throw NOT_IMPL;
  override def userFromStringId(strId: String) = {
    try {
      findById(strId.toInt)
    } catch {
      case e: NumberFormatException => Failure("Failed to parse ID as Long", Full(e), Empty)
    }
  }

  def create(fullName: String, fbId: String): User
  def findById(id: Long): Box[User]
  def findByFbId(fbId: String): Box[User]
  def userRanking(count: Int): Seq[(Option[Int], User)]
  def addPointsForUser(userId: Long, points: Int): Boolean

  onLogIn = List(ExtendedSession.userDidLogin(_))
  onLogOut = List(_ => ExtendedSession.userDidLogout)
}

trait User {
  def id: Long

  def fullName: String
  def fullName_=(str: String): Unit
  def fbId: String
  def fbId_=(id: String): Unit

  def save(): Boolean

  def isAdmin: Boolean

  def firstName: Option[String]
  def firstName_=(firstName: Option[String]): Unit
  def middleName: Option[String]
  def middleName_=(middleName: Option[String]): Unit
  def lastName: Option[String]
  def lastName_=(lastName: Option[String]): Unit
  def gender: Option[String]
  def gender_=(gender: Option[String]): Unit
  def locale: Option[String]
  def locale_=(locale: Option[String]): Unit

  def fbUserName: Option[String]
  def fbUserName_=(fbUserName: Option[String]): Unit

  def fbAccessToken: Option[String]
  def setFbAccessToken(accessToken: Option[String], expiresAt: Option[DateTime]): Unit
  def fbAccessTokenExpires: Option[DateTime]

  def fbTimeZone: Option[String]
  def fbTimeZone_=(fbTimeZone: Option[String]): Unit

  def profilePictureUrl: String = "https://graph.facebook.com/%s/picture".format(fbId)

  def points: Int
  def ranking: Option[Int]
}
