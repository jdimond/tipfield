package de.dimond.tippspiel.model.mapper

import de.dimond.tippspiel.model._

import net.liftweb.mapper._
import net.liftweb.common._

import org.scala_tools.time.Imports._
import java.util.Date

object DbExtendedSession extends DbExtendedSession with ExtendedSession with LongKeyedMetaMapper[DbExtendedSession] {
  import java.util.UUID
  override def saveUserAndGetCookieId(user: User, exp: DateTime): Box[String] = {
    val uuid = UUID.randomUUID()
    val ext = DbExtendedSession.create._userId(user.id)._expires(exp.toDate).saveMe
    Full(ext._cookieId.is)
  }

  override def findUserIdForCookieId(cookieId: String) = {
    find(By(_cookieId, cookieId)) map { ext =>
      (ext._userId.is, new DateTime(ext._expires.is))
    }
  }

  override def deleteCookieId(cookieId: String): Unit = {
    find(By(_cookieId, cookieId)) map { ext => ext.delete_! }
  }
}

class DbExtendedSession extends LongKeyedMapper[DbExtendedSession] {
  def getSingleton = DbExtendedSession

  protected object _id extends MappedLongIndex(this)
  override def primaryKeyField = _id
  protected object _cookieId extends MappedUniqueId(this, 64) {
    override def dbIndexed_? = true
  }
  protected object _userId extends MappedLong(this)
  protected object _expires extends MappedDateTime(this)
}
