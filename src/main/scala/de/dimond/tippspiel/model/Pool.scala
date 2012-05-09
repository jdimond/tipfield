package de.dimond.tippspiel.model

import net.liftweb.common.Box

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
}
