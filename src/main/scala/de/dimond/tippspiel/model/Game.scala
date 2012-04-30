package de.dimond.tippspiel.model

import net.liftweb.common._

import org.scala_tools.time.Imports._
import de.dimond.tippspiel.util.DateHelpers.Implicits._

import de.dimond.tippspiel.model.PersistanceConfiguration._

object Game {
  def all = games.values.toList.sortWith((e1, e2) => (e1.date compareTo e2.date) < 0)
  private var games: Map[Int, Game] = Map()
}

case class Game(id: Int, teamHome: TeamReference, teamAway: TeamReference, date: DateTime, location: Location) {
  Game.games = Game.games + ((id, this))
}

case class Location(location: String)

case class Team(name: String, emblemUrl: String, uefaCoefficient: Int) {
  def reference = DirectTeamReference(this)
}

sealed trait TeamReference {
  def team: Either[String, Team]
  def team_! = team match {
    case Right(team) => team
  }

  def teamAvailable = team isRight
  def asString = team match {
    case Left(str) => str
    case Right(team) => team.name
  }
}

case class DirectTeamReference(theTeam: Team) extends TeamReference {
  override def team = Right(theTeam)
}
case class GroupWinner(group: Group) extends TeamReference {
  override def team = group.winner.toRight("Winner Group %s".format(group.name))
}
case class GroupRunnerUp(group: Group) extends TeamReference {
  override def team = group.runnerUp.toRight("Winner Group %s".format(group.name))
}
case class GameWinner(game: Game) extends TeamReference {
  val noTeam = Left("Winner Match %s".format(game.id))
  override def team = {
    Result.forGame(game) match {
      case Full(result) => {
        val diff = result.goalsHome - result.goalsAway
        if (diff > 0) {
          game.teamHome.team
        } else if (diff < 0) {
          game.teamAway.team
        } else {
          noTeam
        }
      }
      case _ => noTeam
    }
  }
}

object MatchDay {
  private var _matchDays: Map[String, MatchDay] = Map()

  def init(matchDays: Seq[MatchDay]) = _matchDays = _matchDays ++ (for (md <- matchDays) yield (md.id, md))

  def forId(id: String) = _matchDays.getOrElse(id,
                                               throw new IllegalArgumentException("Group not available"))

  def all = _matchDays.values.toList.sortBy(_.firstDate)
}

case class MatchDay(id: String, name: String, games: Seq[Game]) {
  lazy val firstDate = games.map(_.date).min
  lazy val lastDate = games.map(_.date).max
}
