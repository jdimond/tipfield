package de.dimond.tippspiel.model

import net.liftweb.common.Box

import org.scala_tools.time.Imports._

trait MetaTip {
  def updatePoints(result: Result): Boolean
  def forUserAndGame(user: User, game: Game): Box[Tip]
  def forUserAndGames(user: User, games: Seq[Game]): Map[Game, Tip]
  def saveForUserAndGame(user: User, game: Game, goalsHome: Int, goalsAway: Int): Boolean
}

trait Tip {
  def userId: Long
  def gameId: Long
  def goalsHome: Int
  def goalsAway: Int
  def points: Option[Points]
  def submissionTime: DateTime
}

object Points {
  import scala.math._
  def apply(tip: Tip, result: Result): Points = apply(tip.goalsHome, tip.goalsAway, result.goalsHome, result.goalsAway)
  def apply(th: Int, ta: Int, rh: Int, ra: Int): Points = {
    if (th == rh && ta == ra) {
      PointsExact
    } else if (th - ta == rh - ra) {
      PointsSameDifference
    } else if (signum(th - ta) == signum(rh - ra)) {
      PointsTendency
    } else {
      PointsNone
    }
  }
}

sealed trait Points {
  def points: Int
}
case object PointsExact extends Points {
  def points = 3
}
case object PointsSameDifference extends Points {
  def points = 2
}
case object PointsTendency extends Points {
  def points = 1
}
case object PointsNone extends Points {
  def points = 0
}
