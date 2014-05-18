package de.dimond.tippspiel.model

import net.liftweb.common._
import net.liftweb.http.S

import org.scala_tools.time.Imports._

import PersistanceConfiguration._

object Special {
  def all = specials.values.toList.sortBy(_.id)
  def forId(id: Long) = specials(id)
  private var specials: Map[Long, Special] = Map()
}

case class Special(id: Long, title: String, points: Int, answers: List[SpecialAnswer], finalAnswerTime: DateTime) {
  Special.specials = Special.specials + (id -> this)
  def localizedTitle = S.?(title)
}
case class SpecialAnswer(answer: String) {
  def localizedAnswer = S.?(answer)
}

trait MetaSpecialTip {
  def saveForUser(user: User, special: Special, answerNumber: Int): Boolean
  def answerForUser(user: User, special: Special): Box[SpecialTip]
  def answersForUser(user: User, specials: Seq[Special]): Map[Special, SpecialTip]
  def numberPlacedForUser(user: User): Int
  def totalCount: Long
}

trait SpecialTip {
  def userId: Long
  def special: Special
  def answerId: Int
  def submissionTime: DateTime
}

trait MetaSpecialResult {
  def save(special: Special, answerId: Option[Int]): Box[SpecialResult] = {
    val result = doSave(special, answerId)
    result match {
      case Full(result) => {
        if (User.updatePointsAndRanking()) {
          Full(result)
        } else {
          Empty
        }
      }
      case empty => empty
    }
  }
  protected def doSave(special: Special, answerId: Option[Int]): Box[SpecialResult]
  def forSpecial(special: Special): Box[SpecialResult]
}

trait SpecialResult {
  def special: Special
  def answerId: Int
}
