package de.dimond.tippspiel.model.mapper

import de.dimond.tippspiel.model._

import net.liftweb.mapper._
import net.liftweb.common._

import org.scala_tools.time.Imports._
import java.util.Date

object DbFacebookRequests extends DbFacebookRequests with LongKeyedMetaMapper[DbFacebookRequests]
                             with MetaFacebookRequests {
  override def saveRequestForUser(fromUser: User, toFbId: String, requestId: String, poolId: Long) = {
    val request = DbFacebookRequests.create
    request._userId(fromUser.id)
    request._fbId(toFbId)
    request._requestId(requestId)
    request._valid(true)
    request._poolId(poolId)
    request.save()
  }
  override def deleteRequest(fbId: String, requestId: String) = {
    val all = findAll(By(_fbId, fbId), By(_requestId, requestId), By(_valid, true))
    all.map({ req =>
      req._valid(false)
      req.save()
    }).foldLeft(true)(_ & _)
  }
  override def deleteAllRequests(fbId: String): Boolean = {
    val all = findAll(By(_fbId, fbId), By(_valid, true))
    all.map({ req =>
      req._valid(false)
      req.save()
    }).foldLeft(true)(_ & _)
  }
  override def getRequests(fbId: String) = {
    findAll(By(_fbId, fbId), By(_valid, true))
  }
  override def getRequests(fbId: String, poolId: Long) = {
    findAll(By(_fbId, fbId), By(_valid, true), By(_poolId, poolId))
  }
}

class DbFacebookRequests extends LongKeyedMapper[DbFacebookRequests] with IdPK with FacebookRequest {
  def getSingleton = DbFacebookRequests

  object _userId extends MappedLong(this) {
    override def dbIndexed_? = true
  }
  object _fbId extends MappedString(this, 16) {
    override def dbIndexed_? = true
  }
  object _requestId extends MappedString(this, 32) {
    override def dbIndexed_? = true
  }
  object _valid extends MappedBoolean(this)

  object _poolId extends MappedLong(this) {
    override def dbIndexed_? = true
  }

  override def requestId = _requestId.is
  override def poolId = _poolId.is
  override def fromUserId = _userId.is
  override def forFbId = _fbId.is
}
