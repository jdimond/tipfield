package de.dimond.tippspiel.model

import net.liftweb.common._

import org.scala_tools.time.Imports._
import de.dimond.tippspiel.util.DateHelpers.Implicits._

import de.dimond.tippspiel.model.PersistanceConfiguration._
import net.liftweb.http.S

object Game {
  def all = games.values.toList.sortWith((e1, e2) => (e1.date compareTo e2.date) < 0)
  def forId(gameId: Long) = games.get(gameId)
  private var games: Map[Long, Game] = Map()
}

case class Game(id: Long, teamHome: TeamReference, teamAway: TeamReference, date: DateTime, location: Location) {
  Game.games = Game.games + ((id, this))
}

case class Location(location: String) {
  def localizedName = S.?(location)
}

case class Team(name: String, emblemUrl: String, uefaCoefficient: Int) {
  def reference = DirectTeamReference(this)
  def localizedName = S.?(name)
}

sealed trait TeamReference {
  def team: Either[(String, String), Team]
  def team_! = team match {
    case Right(team) => team
  }

  def teamAvailable = team isRight
  def asString = team match {
    case Left((str, id)) => S.?(str).format(id)
    case Right(team) => team.localizedName
  }
}

case class DirectTeamReference(theTeam: Team) extends TeamReference {
  override def team = Right(theTeam)
}
case class GroupWinner(group: Group) extends TeamReference {
  override def team = group.winner.toRight(("winner_group_x", group.name))
}
case class GroupRunnerUp(group: Group) extends TeamReference {
  override def team = group.runnerUp.toRight(("runner_up_group_x", group.name))
}
case class GameWinner(game: Game) extends TeamReference {
  val noTeam = Left(("winner_match_x", game.id.toString))
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
  def localizedName = S.?(name)
}
