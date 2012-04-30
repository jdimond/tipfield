package de.dimond.tippspiel.model.mapper

import de.dimond.tippspiel.model._

import net.liftweb.mapper._
import net.liftweb.common._

import org.scala_tools.time.Imports._
import java.util.Date

object DbTip extends DbTip with LongKeyedMetaMapper[DbTip] with MetaTip {
  def updatePoints(result: Result): Boolean = false
  def forUserAndGame(user: User, game: Game): Box[Tip] = find(By(_userId, user.id), By(_gameId, game.id))
  def saveForUserAndGame(user: User, game: Game, goalsHome: Int, goalsAway: Int): Boolean = {
    val tip = find(By(_userId, user.id), By(_gameId, game.id)) openOr DbTip.create._userId(user.id)._gameId(game.id)
    tip._goalsHome(goalsHome)
    tip._goalsAway(goalsAway)
    return tip.save()
  }
}

class DbTip extends Tip with LongKeyedMapper[DbTip] with IdPK {
  def getSingleton = DbTip

  protected object _userId extends MappedLong(this) {
    override def dbIndexed_? = true
  }
  protected object _gameId extends MappedLong(this) {
    override def dbIndexed_? = true
  }
  protected object _goalsHome extends MappedInt(this)
  protected object _goalsAway extends MappedInt(this)
  protected object _points extends MappedInt(this)
  protected object _submissionTime extends MappedDateTime(this)

  def userId = _userId.is
  def gameId = _gameId.is
  def goalsHome = _goalsHome.is
  def goalsAway = _goalsAway.is
  def points = None
  def submissionTime = new DateTime(_submissionTime.is)
}
