package de.dimond.tippspiel.model.mapper

import de.dimond.tippspiel.model._

import net.liftweb.mapper._
import net.liftweb.common._

import org.scala_tools.time.Imports._
import java.util.Date

object DbSpecialTip extends DbSpecialTip with LongKeyedMetaMapper[DbSpecialTip] with MetaSpecialTip {
  def updatePoints(special: Special, finalAnswerId: Int): Boolean = false
  def answerForUser(user: User, special: Special) = find(By(_userId, user.id), By(_specialId, special.id))
  def answersForUser(user: User, specials: Seq[Special]): Map[Special, SpecialTip] =  {
    val specialTips = findAll(By(_userId, user.id), ByList(_specialId, specials.map(_.id)))
    val tipMap = specialTips.map(tip => (tip._specialId.is, tip)).toMap
    val seq = for {
      special <- specials
      tip <- tipMap.get(special.id)
    } yield (special -> tip)
    seq.toMap
  }
  def saveForUser(user: User, special: Special, answerId: Int): Boolean = {
    if (DateTime.now > special.finalAnswerTime) {
      return false
    }
    val box = find(By(_userId, user.id), By(_specialId, special.id))
    val specialAnswer = box openOr DbSpecialTip.create._userId(user.id)._specialId(special.id)
    specialAnswer._answerId(answerId)
    specialAnswer._submissionTime(new Date())
    specialAnswer.save()
  }
}

class DbSpecialTip extends SpecialTip with LongKeyedMapper[DbSpecialTip] with IdPK {
  def getSingleton = DbSpecialTip

  protected object _userId extends MappedLong(this) {
    override def dbIndexed_? = true
  }
  protected object _specialId extends MappedLong(this) {
    override def dbIndexed_? = true
  }
  protected object _answerId extends MappedInt(this)
  protected object _submissionTime extends MappedDateTime(this)
  protected object _points extends MappedInt(this)

  override def userId = _userId.is
  override def special = Special.forId(_specialId.is)
  override def answerId = _answerId.is
  override def submissionTime = new DateTime(_submissionTime.is)
}
