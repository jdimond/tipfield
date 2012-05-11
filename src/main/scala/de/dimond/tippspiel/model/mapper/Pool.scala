package de.dimond.tippspiel.model.mapper

import de.dimond.tippspiel.model._

import net.liftweb.mapper._
import net.liftweb.common._

import org.scala_tools.time.Imports._
import java.util.Date

object DbPool extends DbPool with LongKeyedMetaMapper[DbPool] with MetaPool {
  override def newPool(name: String, description: String, allowMemberInvite: Boolean, admin: User): Box[Pool] = {
    val pool = DbPool.create
    pool._name(name)
    pool._description(description)
    pool._allowMemberInvite(allowMemberInvite)
    pool._adminId(admin.id)
    if (pool.save) {
      if (pool.inviteUser(admin.fbId, None) && pool.addUser(admin)) {
        Full(pool)
      } else {
        pool.delete_!
        Failure("Failed to add admin")
      }
    } else {
      Failure("Database Error")
    }
  }
  override def allForUser(user: User): Set[Pool] = {
    val poolIds = DbPoolMembership.findAll(By(DbPoolMembership.userId, user.id),
                                           By(DbPoolMembership.hasLeft, false)).map(_.pool.is)
    poolIds.map(forId(_)).flatten.toSet
  }
  override def forId(poolId: Long) = find(By(_id, poolId))

  override def invitationsForUser(user: User, withIgnored: Boolean, withActive: Boolean): Set[Pool] = {
    val invitations = DbPoolInvites.findAll(By(DbPoolInvites.fbId, user.fbId))
    val filtered = invitations.filter(withIgnored || !_.ignored.is)
    if (withActive) {
      filtered.map(i => forId(i.pool.is)).flatten.toSet
    } else {
      val poolIds = DbPoolMembership.findAll(By(DbPoolMembership.userId, user.id),
                                             By(DbPoolMembership.hasLeft, false)).map(_.pool.is)
      filtered.filter(i => !poolIds.contains(i.pool.is)).map(i => forId(i.pool.is)).flatten.toSet
    }
  }
}

class DbPool extends Pool with LongKeyedMapper[DbPool] with Logger {
  def getSingleton = DbPool

  protected object _id extends MappedLongIndex(this)
  override def primaryKeyField = _id

  protected object _name extends MappedString(this, 64)
  protected object _description extends MappedString(this, 1024)
  protected object _adminId extends MappedLong(this)
  protected object _allowMemberInvite extends MappedBoolean(this)

  def id = _id.is
  def name = _name.is
  def description = _description.is
  def adminId = _adminId.is

  def removeUser(user: User): Boolean = {
    val pools = DbPoolMembership.findAll(By(DbPoolMembership.userId, user.id), By(DbPoolMembership.pool, this))
    val successMembership = pools.map(_.hasLeft(true).save()).reduce(_ && _)
    val invites = DbPoolInvites.findAll(By(DbPoolInvites.fbId, user.fbId), By(DbPoolInvites.pool, this))
    val successInvites = invites.map(_.ignored(true).save()).reduce(_ && _)
    return successInvites && successMembership
  }

  def addUser(user: User): Boolean = {
    if (!userIsInvited(user.fbId)) {
      warn("Trying to add user without invitation!")
      return false
    }
    val membership =  DbPoolMembership.create
    membership.userId(user.id)
    membership.pool(this)
    membership.hasLeft(false)
    return membership.save()
  }

  override def users = {
    val memberships = DbPoolMembership.findAll(By(DbPoolMembership.pool, this), By(DbPoolMembership.hasLeft, false))
    memberships.map(_.userId.is).toSet
  }

  override def userHasLeftGroup(userId: Long) = {
    DbPoolMembership.find(By(DbPoolMembership.pool, this), By(DbPoolMembership.userId, userId)).map(_.hasLeft.is)
  }

  override def userIsAllowedToInvite(user: User) = _allowMemberInvite.is || (user.id == _adminId.is)

  override def inviteUser(facebookId: String, fromUser: Option[User]) = {
    val invitingUserId = fromUser match {
      case Some(user) => {
        if (!userIsAllowedToInvite(user)) {
          throw new IllegalArgumentException("Inviting user is not allowed to make invitations!")
        }
        user.id
      }
      case None => 0
    }
    val inviteBox = DbPoolInvites.find(By(DbPoolInvites.pool, this),
                                       By(DbPoolInvites.fbId, facebookId),
                                       By(DbPoolInvites.invitingUserId, invitingUserId))
    val invite = inviteBox openOr DbPoolInvites.create
    invite.invitingUserId(invitingUserId)
    invite.fbId(facebookId)
    invite.pool(this)
    invite.ignored(false)
    invite.save()
  }
  override def userIsInvited(facebookId: String) = {
    (DbPoolInvites.count(By(DbPoolInvites.pool, this), By(DbPoolInvites.fbId, facebookId)) > 0)
  }
  override def ignoreInvitations(user: User) = {
    val invitations = DbPoolInvites.findAll(By(DbPoolInvites.pool, this), By(DbPoolInvites.fbId, user.fbId))
    invitations.map { invitation =>
      invitation.ignored(true)
      invitation.save()
    }
  }
}

object DbPoolMembership extends DbPoolMembership with LongKeyedMetaMapper[DbPoolMembership]

class DbPoolMembership extends LongKeyedMapper[DbPoolMembership] with IdPK {
  def getSingleton = DbPoolMembership

  object userId extends MappedLong(this)
  object pool extends MappedLongForeignKey(this, DbPool)
  object hasLeft extends MappedBoolean(this)
}

object DbPoolInvites extends DbPoolInvites with LongKeyedMetaMapper[DbPoolInvites]

class DbPoolInvites extends LongKeyedMapper[DbPoolInvites] with IdPK {
  def getSingleton = DbPoolInvites

  object invitingUserId extends MappedLong(this)
  object fbId extends MappedString(this, 16)
  object pool extends MappedLongForeignKey(this, DbPool)
  object ignored extends MappedBoolean(this)
}
