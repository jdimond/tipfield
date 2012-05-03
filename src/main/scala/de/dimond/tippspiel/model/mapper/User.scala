package de.dimond.tippspiel.model.mapper

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._

import net.liftweb.mapper._
import net.liftweb.common._

import org.scala_tools.time.Imports._
import java.util.Date

object DbUser extends DbUser with LongKeyedMetaMapper[DbUser] with MetaUser[DbUser] with Logger {
  override def create(fullName: String, fbId: String): User = {
    if (count(By(_fbId, fbId)) > 0) {
      throw new IllegalArgumentException("Can't readd user to database with same id!")
    }
    val user: DbUser = DbUser.create
    user.fullName = fullName
    user.fbId = fbId
    user
  }
  override def findById(id: Long): Box[User] = find(By(_id, id))
  override def findByFbId(fbId: String): Box[User] = find(By(_fbId, fbId))
  override def userRanking(count: Int): Seq[(Rank, User)] = Seq()
  override def addPointsForUser(userId: Long, points: Int): Boolean = false

  override def afterCreate = updateUserIdForFriends _ :: super.afterCreate

  override def save(user: DbUser) = {
    if (super.save(user)) {
      updateFacebookFriends(user)
    } else {
      false
    }
  }

  private def updateUserIdForFriends(user: DbUser): Unit = {
    val allFriends = DbFriends.findAll(By(DbFriends.friendFacebookId, user.fbId))
    for (friend <- allFriends) {
      friend.friendUserId(user.id).save()
    }
  }

  private def updateFacebookFriends(user: DbUser): Boolean = {
    if (user._facebookFriendsDirty) {
      user._facebookFriends match {
        case Some(ids) => {
          var success = true
          val allForUser = DbFriends.findAll(By(DbFriends.userId, user.id))
          /* validate/invalidate in the database */
          for (friend <- allForUser) {
            if (ids.contains(friend.friendFacebookId.is)) {
              if (!friend.valid.is) {
                success = success && friend.valid(true).save()
              }
            } else {
              if (friend.valid.is) {
                success = success && friend.valid(false).save()
              }
            }
          }
          /* create new friend links for new entries */
          val oldIds = allForUser.map(_.friendFacebookId.is).toSet
          val newIds = ids.filter(!oldIds.contains(_))
          for (newId <- newIds) {
            val friend = DbFriends.create
            friend.userId(user.id)
            friend.friendFacebookId(newId)
            friend.valid(true)
            User.findByFbId(newId) match {
              case Full(u) => friend.friendUserId(u.id)
              case _ => /* ignore */
            }
            success = success && friend.save()
          }
          success
        }
        case None => throw new IllegalStateException("This shouldn't happen!")
      }
    } else {
      true
    }
  }
}

class DbUser extends User with LongKeyedMapper[DbUser] with Logger {
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

  override def id: Long = _id.is

  override def fullName: String = _fullName.is
  override def fullName_=(str: String) = _fullName(str)
  override def fbId: String = _fbId.is
  override def fbId_=(id: String) = _fbId(id)

  override def isAdmin: Boolean = _admin.is

  override def firstName = Some(_firstName.is)
  override def firstName_=(firstName: Option[String]) = _firstName(firstName)
  override def middleName = Some(_middleName.is)
  override def middleName_=(middleName: Option[String]) = _middleName(middleName)
  override def lastName = Some(_lastName.is)
  override def lastName_=(lastName: Option[String]) = _lastName(lastName)
  override def gender = Some(_gender.is)
  override def gender_=(gender: Option[String]) = _gender(gender)
  override def locale = Some(_locale.is)
  override def locale_=(locale: Option[String]) = _locale(locale)

  override def fbUserName= Some(_fbUserName.is)
  override def fbUserName_=(fbUserName: Option[String]) = _fbUserName(fbUserName)

  override def fbAccessToken = Some(_fbAccessToken)
  override def setFbAccessToken(accessToken: Option[String], expiresAt: Option[DateTime]) = {
    accessToken match {
      case Some(a) => _fbAccessToken(a)
      case None => _fbAccessToken("")
    }
    expiresAt match {
      case Some(d) => _fbAccessTokenExpires(d.toDate())
      case None => _fbAccessTokenExpires(new Date())
    }
  }
  override def fbAccessTokenExpires = Some(new DateTime(_fbAccessTokenExpires.is))

  override def fbTimeZone = Some(_fbTimeZone)
  override def fbTimeZone_=(fbTimeZone: Option[String]) = _fbTimeZone(fbTimeZone)

  override def points = _points.is
  override def ranking = Some(_ranking.is)

  private var _facebookFriends: Option[Set[String]] = None
  private var _facebookFriendsDirty = false

  override def facebookFriends = _facebookFriends match {
    case Some(friends) => friends
    case None => {
      val dbFriends = DbFriends.findAll(By(DbFriends.userId, this.id), By(DbFriends.valid, true))
      val friendIds = dbFriends.map(_.friendFacebookId.is).toSet
      _facebookFriends = Some(friendIds)
      friendIds
    }
  }

  override def facebookFriends_=(ids: Set[String]) = {
    _facebookFriendsDirty = true
    _facebookFriends = Some(ids)
  }

  override def friends: Set[Long] = {
    val friends = DbFriends.findAll(By(DbFriends.userId, this.id), By_>(DbFriends.friendUserId, 0), By(DbFriends.valid,
    true))
    friends.map(_.friendUserId.is).toSet
  }
}

object DbFriends extends DbFriends with LongKeyedMetaMapper[DbFriends]

class DbFriends extends LongKeyedMapper[DbFriends] with IdPK {
  def getSingleton = DbFriends

  object userId extends MappedLong(this) {
    override def dbIndexed_? = true
  }
  object friendFacebookId extends MappedString(this, 16) {
    override def dbIndexed_? = true
  }
  object friendUserId extends MappedLong(this) {
    override def dbIndexed_? = true
  }
  object valid extends MappedBoolean(this)
}
