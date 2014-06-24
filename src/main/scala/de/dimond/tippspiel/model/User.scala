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
import de.dimond.tippspiel.util.Util

sealed trait Rank {
  def is: Int
}

case class PrimaryRank(is: Int) extends Rank
case class SecondaryRank(is: Int) extends Rank

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
      case e: NumberFormatException => Failure("Failed to parse \"%s\" as Long".format(strId), Full(e), Empty)
    }
  }

  def create(fullName: String, fbId: String): User
  def findById(id: Long): Box[User]
  def findByFbId(fbId: String): Box[User]
  def findAll(ids: Set[Long]): Seq[User]
  def findAll(): Seq[User]
  def userRanking(count: Int): Seq[(Rank, User)]

  def totalCount: Long

  def rankUsers(users: Seq[User]): Seq[(Rank, User)] = {
    val stableSorted = users.groupBy(_.points).toSeq.sortBy(_._1).reverse.map(_._2).flatten
    stableSorted.foldLeft((1, 0, Int.MaxValue, List(): List[(Rank, User)]))({
      case ((counter, currentRank, oldPoints, list), user) => {
        if (user.points==oldPoints) {
          ((counter + 1), currentRank, user.points, ((SecondaryRank(currentRank), user)) :: list)
        } else {
          ((counter + 1), counter, user.points, ((PrimaryRank(counter), user)) :: list)
        }
      }
    })._4.reverse
  }

  def updatePointsAndRanking(): Boolean = {
    val users = findAll()
    val updatedPoints = users.map(_.updatePoints).foldLeft(true)(_ && _)
    if (updatedPoints) {
      val totalRanking = rankUsers(users)
      val rankingSuccess = totalRanking.map { case (rank, user) =>
        user.ranking = Some(rank.is)
        user.save()
      }.foldLeft(true)(_ && _)
      if (rankingSuccess) {
        true
      } else {
        warn("Failed to update user ranking!")
        false
      }
    } else {
      warn("Failed to update user points!")
      false
    }
  }

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

  def facebookFriends: Set[String]
  def facebookFriends_=(ids: Set[String]): Unit

  def friends: Set[Long]
  def poolFriends: Set[Long]

  def points: Int
  protected def points_=(p: Int): Unit

  def ranking: Option[Int]
  def ranking_=(r: Option[Int]): Unit

  def numberOfTips: Int
  def numberOfTips_=(n: Int): Unit
  def numberOfSpecials: Int
  def numberOfSpecials_=(n: Int): Unit

  def updatePoints() = {
    val tips = Tip.forUserAndGames(this, Game.all)
    val pointsTips = for {
      tip <- tips.values
      points <- tip.points
    } yield points.points
    val specials = SpecialTip.answersForUser(this, Special.all)
    val pointsSpecials: Seq[Int] = for {
      specialTip <- specials.values.toSeq
      result <- SpecialResult.forSpecial(specialTip.special)
      val points = result.special.points if specialTip.answerId == result.answerId
    } yield points
    this.points = pointsTips.sum + pointsSpecials.sum
    this.save
  }
}
