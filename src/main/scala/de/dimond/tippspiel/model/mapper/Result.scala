package de.dimond.tippspiel.model.mapper

import de.dimond.tippspiel.model._

import net.liftweb.mapper._
import net.liftweb.common._

import org.scala_tools.time.Imports._
import java.util.Date

object DbResult extends DbResult with LongKeyedMetaMapper[DbResult] with MetaResult {
  def forGame(game: Game): Box[Result] = find(By(_gameId, game.id))
  def saveForGame(game: Game, goalsHome: Int, goalsAway: Int): Box[Result] = {
    val result = find(By(_gameId, game.id)) openOr DbResult.create
    result._gameId(game.id)
    result._goalsHome(goalsHome)
    result._goalsAway(goalsAway)
    if (result.save) {
      Full(result)
    } else {
      Failure("There was a database error")
    }
  }
}

class DbResult extends Result with LongKeyedMapper[DbResult]{
  def getSingleton = DbResult

  override def primaryKeyField = _gameId

  protected object _gameId extends MappedLongIndex(this)
  protected object _goalsHome extends MappedInt(this)
  protected object _goalsAway extends MappedInt(this)

  def gameId = _gameId.is
  def goalsHome = _goalsHome.is
  def goalsAway = _goalsAway.is
}
