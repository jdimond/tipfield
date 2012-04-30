package de.dimond.tippspiel.model

import net.liftweb.common.Box

import org.scala_tools.time.Imports._

trait MetaResult {
  def forGame(game: Game): Box[Result]
  def saveForGame(game: Game, goalsHome: Int, goalsAway: Int): Box[Result]
}

trait Result {
  def gameId: Long
  def goalsHome: Int
  def goalsAway: Int
}
