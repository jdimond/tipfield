package de.dimond.tippspiel.model

import net.liftweb.common._
import net.liftweb.http.S._
import PersistanceConfiguration._

import org.scala_tools.time.Imports._

trait MetaPool {
  def newPool(name: String, description: String, allowMemberInvite: Boolean, admin: User): Box[Pool]
  def allForUser(user: User): Set[Pool]
  def forId(poolId: Long): Box[Pool]
  def invitationsForUser(user: User, withIgnored: Boolean = false, withActive: Boolean = false): Set[Pool]
}

trait Pool {
  def id: Long
  def name: String
  def description: String
  def users: Set[Long]
  def adminId: Long

  def removeUser(user: User): Boolean
  def addUser(user: User): Boolean

  def userHasLeftGroup(userId: Long): Box[Boolean]
  def inviteUser(facebookId: String, fromUser: Option[User]): Boolean
  def userIsAllowedToInvite(user: User): Boolean
  def userIsInvited(facebookId: String): Boolean
  def ignoreInvitations(user: User): Unit
  def updateDescription(description: String): Unit
}

case object FacebookPool extends Pool {
  def id = 0
  def name = ?("facebook_friends")
  def description = ""
  def users = User.currentUser match {
    case Full(user) => user.friends + user.id
    case _ => Set()
  }
  def adminId = 0
  def removeUser(user: User) = throw new RuntimeException("Not supported")
  def addUser(user: User) = throw new RuntimeException("Not supported")

  def userHasLeftGroup(userId: Long) = Full(false)
  def inviteUser(facebookId: String, fromUser: Option[User]) = throw new RuntimeException("Not supported")
  def userIsAllowedToInvite(user: User) = true
  def userIsInvited(facebookId: String) = true
  def ignoreInvitations(user: User) = throw new RuntimeException("Not supported")
  def updateDescription(description: String) = throw new RuntimeException("Not supported")
}

trait MetaFacebookRequests {
  def saveRequestForUser(fromUser: User, toFbId: String, requestId: String, poolId: Long): Boolean
  def deleteRequest(fbId: String, requestId: String): Boolean
  def deleteAllRequests(fbId: String): Boolean
  def getRequests(fbId: String): List[FacebookRequest]
  def getRequests(fbId: String, poolId: Long): List[FacebookRequest]
}

trait FacebookRequest {
  def requestId: String
  def poolId: Long
  def fromUserId: Long
  def forFbId: String
}

trait PoolComment {
  def commentDate: DateTime
  def commentId: Long
  def poolId: Long
  def userId: Long
  def comment: String
}

trait MetaPoolComment {
  def saveComment(pool: Pool, user: User, comment: String): Box[PoolComment]
  def commentsForPool(pool: Pool): Seq[PoolComment]
}
