package de.dimond.tippspiel.model

import de.dimond.tippspiel.model.PersistanceConfiguration._

object Group {
  private var _groupMap: Map[String, Group] = Map()
  private var _groups: Seq[Group] = Seq()

  def init(groups: Seq[Group]) = {
    _groupMap = _groupMap ++ (for (group <- groups) yield (group.name, group))
    _groups = _groups ++ groups
  }

  def forName(groupName: String) = _groupMap.getOrElse(groupName,
                                                       throw new IllegalArgumentException("Group not available"))

  def all = _groups
}

trait Group {
//(name: String, teams: Seq[Team], games: Seq[Game]) {
  for (game <- games) {
    if (!teams.contains(game.teamHome.team_!)) {
      throw new IllegalArgumentException("Group does not contain Team " + game.teamHome.team)
    }
    if (!teams.contains(game.teamAway.team_!)) {
      throw new IllegalArgumentException("Group does not contain Team " + game.teamAway.team)
    }
  }

  val initialStandings = for (team <- teams) yield Standing(team, 0, 0, 0, 0, 0, 0, 0)

  private def isFinished = games.filter(Result.forGame(_).isEmpty).size == 0

  def name: String
  def teams: Seq[Team]
  def games: Seq[Game]

  def winner: Option[Team] = {
    if (isFinished) Some(standings(0).team)
    else None
  }

  def runnerUp: Option[Team] = {
    if (isFinished) Some(standings(1).team)
    else None
  }

  protected def unsortedStandings: Seq[Standing] = {
    val results = games.map(game => ((game, Result.forGame(game))))
    val standingsAgg = for { resultBox <- results; result <- resultBox._2; game = resultBox._1 } yield {
      val gh = result.goalsHome
      val ga = result.goalsAway
      val teamHome = game.teamHome.team
      val teamAway = game.teamAway.team
      (teamHome, teamAway) match {
        case (Right(th), Right(ta)) => {
          (gh - ga) match {
            case 0 => Seq(Standing(th, 1, 0, 1, 0, gh, ga, 1), Standing(ta, 1, 0, 1, 0, ga, gh, 1))
            case x if (x > 0) => Seq(Standing(th, 1, 1, 0, 0, gh, ga, 3), Standing(ta, 1, 0, 0, 1, ga, gh, 0))
            case _ => Seq(Standing(th, 1, 0, 0, 1, gh, ga, 0), Standing(ta, 1, 1, 0, 0, ga, gh, 3))
          }
        }
        case _ => Seq()
      }
    }
    (initialStandings ++ standingsAgg.flatten).groupBy(_.team).values.map(_.reduce(_ + _)).toSeq
  }
  def standings: Seq[Standing]
}

case class Standing(team: Team, gamesPlayed: Int, won: Int, drawn: Int, lost: Int, goalsScored: Int,
                    goalsReceived: Int, points: Int) extends Ordered[Standing] {
  def +(s: Standing) = {
    if (s.team != team) {
      throw new IllegalArgumentException("Teams do not match: %s != %s".format(team, s.team))
    }
    Standing(team, gamesPlayed + s.gamesPlayed, won + s.won, drawn + s.drawn, lost + s.lost,
             goalsScored + s.goalsScored, goalsReceived + s.goalsReceived, points + s.points)
  }

  def goalDifference: Int = this.goalsScored - this.goalsReceived

  override def compare(that: Standing) = {
    (this.points - that.points) match {
      case x if x > 0 => -1
      case x if x < 0 => 1
      case _ => (this.goalDifference - that.goalDifference) match {
        case y if y > 0 => -1
        case y if y < 0 => 1
        case y => that.goalsScored - this.goalsScored
      }
    }
  }
}
