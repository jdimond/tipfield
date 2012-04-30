package de.dimond.tippspiel.model.mapper

import de.dimond.tippspiel.model._

import net.liftweb.mapper._
import net.liftweb.common._

import org.scala_tools.time.Imports._
import java.util.Date

object DbUser extends DbUser with LongKeyedMetaMapper[DbUser] with MetaUser[DbUser] with Logger {
  def create(fullName: String, fbId: String): User = {
    val user: DbUser = DbUser.create
    user.fullName = fullName
    user.fbId = fbId
    user
  }
  def findById(id: Long): Box[User] = find(By(_id, id))
  def findByFbId(fbId: String): Box[User] = find(By(_fbId, fbId))
  def userRanking(count: Int): Seq[(Option[Int], User)] = Seq()
  def addPointsForUser(userId: Long, points: Int): Boolean = false
}

class DbUser extends User with LongKeyedMapper[DbUser] {
  def getSingleton = DbUser

  protected object _id extends MappedLongIndex(this)
  override def primaryKeyField = _id

  protected object _fullName extends MappedString(this, 128) {
    override def required_? = true
  }
  protected object _fbId extends MappedString(this, 16) {
    override def required_? = true
  }
  protected object _admin extends MappedBoolean(this) {
    override def required_? = true
  }

  protected object _firstName extends MappedString(this, 64)
  protected object _middleName extends MappedString(this, 64)
  protected object _lastName extends MappedString(this, 64)
  protected object _gender extends MappedString(this, 16)
  protected object _locale extends MappedString(this, 16)

  protected object _fbUserName extends MappedString(this, 128)
  protected object _fbAccessToken extends MappedString(this, 256)
  protected object _fbAccessTokenExpires extends MappedDateTime(this)
  protected object _fbTimeZone extends MappedString(this, 16)

  protected object _points extends MappedInt(this) {
    override def dbIndexed_? = true
  }
  protected object _ranking extends MappedInt(this) {
    override def dbIndexed_? = true
  }

  implicit def optionToDefault(o: Option[String]): String = {
    o match {
      case Some(str) => str
      case None => ""
    }
  }

  def id: Long = _id.is

  def fullName: String = _fullName.is
  def fullName_=(str: String) = _fullName(str)
  def fbId: String = _fbId.is
  def fbId_=(id: String) = _fbId(id)

  //def save(): Boolean

  def isAdmin: Boolean = _admin.is

  def firstName = Some(_firstName.is)
  def firstName_=(firstName: Option[String]) = _firstName(firstName)
  def middleName = Some(_middleName.is)
  def middleName_=(middleName: Option[String]) = _middleName(middleName)
  def lastName = Some(_lastName.is)
  def lastName_=(lastName: Option[String]) = _lastName(lastName)
  def gender = Some(_gender.is)
  def gender_=(gender: Option[String]) = _gender(gender)
  def locale = Some(_locale.is)
  def locale_=(locale: Option[String]) = _locale(locale)

  def fbUserName= Some(_fbUserName.is)
  def fbUserName_=(fbUserName: Option[String]) = _fbUserName(fbUserName)

  def fbAccessToken = Some(_fbAccessToken)
  def setFbAccessToken(accessToken: Option[String], expiresAt: Option[DateTime]) = {
    accessToken match {
      case Some(a) => _fbAccessToken(a)
      case None => _fbAccessToken("")
    }
    expiresAt match {
      case Some(d) => _fbAccessTokenExpires(d.toDate())
      case None => _fbAccessTokenExpires(new Date())
    }
  }
  def fbAccessTokenExpires = Some(new DateTime(_fbAccessTokenExpires.is))

  def fbTimeZone = Some(_fbTimeZone)
  def fbTimeZone_=(fbTimeZone: Option[String]) = _fbTimeZone(fbTimeZone)

  def points = _points.is
  def ranking = Some(_ranking.is)
}
