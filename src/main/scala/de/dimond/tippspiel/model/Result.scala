package de.dimond.tippspiel.model

import net.liftweb.common._

import org.scala_tools.time.Imports._

import PersistanceConfiguration._

trait MetaResult extends Logger {
  def forGame(game: Game): Box[Result]
  def saveForGame(game: Game, goalsHome: Int, goalsAway: Int): Box[Result] = {
    val result = doSaveForGame(game, goalsHome, goalsAway)
    result match {
      case Full(result) => {
        if (Tip.updatePoints(result) && User.updatePointsAndRanking()) {
          Full(result)
        } else {
          Empty
        }
      }
      case empty => empty
    }
  }
  protected def doSaveForGame(game: Game, goalsHome: Int, goalsAway: Int): Box[Result]
}

trait Result {
  def gameId: Long
  def goalsHome: Int
  def goalsAway: Int
}
