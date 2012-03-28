package de.dimond.tippspiel.model

import org.scala_tools.time.Imports._
import de.dimond.tippspiel.util.DateHelpers.Implicits._

import net.liftweb.mapper._
import net.liftweb.common._

case class Standing(team: Team, gamesPlayed: Int, won: Int, drawn: Int, lost: Int, goalsScored: Int,
                    goalsReceived: Int, points: Int) extends Ordered[Standing] {
  def +(s: Standing) = {
    if (s.team != team) {
      throw new IllegalArgumentException("Teams do not match: %s != %s".format(team, s.team))
    }
    Standing(team, gamesPlayed + s.gamesPlayed, won + s.won, drawn + s.drawn, lost + s.lost,
             goalsScored + s.goalsScored, goalsReceived + s.goalsReceived, points + s.points)
  }

  override def compare(that: Standing) = {
    (this.points - that.points) match {
      case x if x > 0 => -1
      case x if x < 0 => 1
      case _ => ((this.goalsScored - this.goalsReceived) - (that.goalsScored - that.goalsReceived)) match {
        case y if y > 0 => -1
        case y if y < 0 => 1
        case y => that.goalsScored - this.goalsScored
      }
    }
  }
}

object Group {
  private var _groups: Map[String, Group] = Map()

  def init(groups: Seq[Group]) = _groups = _groups ++ (for (group <- groups) yield (group.name, group))

  def forName(groupName: String) = _groups.getOrElse(groupName,
                                                     throw new IllegalArgumentException("Group not available"))

  def all = _groups.values
}

case class Group(name: String, teams: Seq[Team], games: Seq[Game]) {
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

  def winner: Option[Team] = {
    if (isFinished) Some(standings(0).team)
    else None
  }

  def runnerUp: Option[Team] = {
    if (isFinished) Some(standings(1).team)
    else None
  }

  def standings: Seq[Standing] = {
    val results = games.map(game => ((game, Result.forGame(game))))
    val standingsAgg = for { resultBox <- results; result <- resultBox._2; game = resultBox._1 } yield {
      val gh = result.goalsHome.is
      val ga = result.goalsAway.is
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
    val standings = (initialStandings ++ standingsAgg.flatten).groupBy(_.team).values.map(_.reduce(_ + _))
    lazy val standingsTieBreaker = standings.groupBy(_.points).values.filter(_.size > 1).map(x => {
        val teams = x.map(_.team).toList
        /* Check if we would run into endless recursion */
        if (teams.size < this.teams.size) {
          Group("", teams, games.filter(g => (g.teamAway.team, g.teamHome.team) match {
              case (Right(a), Right(b)) => teams.contains(a) && teams.contains(b)
              case _ => false
          })).standings
        } else {
          Seq()
        }
      }).flatten

    standings.toList.sortWith((x, y) => {
      (x.points > y.points) || (x.points == y.points && {
        val x2 = standingsTieBreaker.filter(s => s.team == x.team).headOption
        val y2 = standingsTieBreaker.filter(s => s.team == y.team).headOption
        val tiebreaker = (x2, y2) match {
          case (Some(x3), Some(y3)) => x3.compareTo(y3)
          case _ => 0
        }
        (tiebreaker < 0) || (tiebreaker == 0 && {
          val diffX = x.goalsScored - x.goalsReceived
          val diffY = y.goalsScored - y.goalsReceived
          (diffX > diffY || (diffX == diffY && (
            (x.goalsScored > y.goalsScored) || ((x.goalsScored == y.goalsScored) &&
              x.team.uefaCoefficient > y.team.uefaCoefficient
            )
          )))
        })
      })
    })
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

object Game {
  def all = games.values.toList.sortWith((e1, e2) => (e1.date compareTo e2.date) < 0)
  private var games: Map[Int, Game] = Map()
}

case class Location(location: String)

case class Game(id: Int, teamHome: TeamReference, teamAway: TeamReference, date: DateTime, location: Location) {
  Game.games = Game.games + ((id, this))
}

case class Team(name: String, emblemUrl: String, uefaCoefficient: Int) {
  def reference = DirectTeamReference(this)
}

object Tip extends Tip with LongKeyedMetaMapper[Tip] {
  def findByGame(user: User, game: Game) = {
    Tip find (By(Tip.user, user), By(Tip.gameId, game.id))
  }
}

class Tip extends LongKeyedMapper[Tip] with IdPK {
  def getSingleton = Tip
  object user extends MappedLongForeignKey(this, User)
  object gameId extends MappedLong(this)
  object goalsHome extends MappedInt(this)
  object goalsAway extends MappedInt(this)
}

object Result extends Result with LongKeyedMetaMapper[Result] {
  def goalsForGame(game: Game) = forGame(game) map { _.goals } openOr ""
  def forGame(game: Game) = find(By(Result.gameId, game.id))
}

class Result extends LongKeyedMapper[Result] with IdPK {
  def getSingleton = Result
  object gameId extends MappedLong(this)
  object goalsHome extends MappedInt(this)
  object goalsAway extends MappedInt(this)

  def goals = goalsHome.is + " : " + goalsAway.is
}

object User extends User with MetaMegaProtoUser[User] {
  def findByFbId(id: String) = find(By(User.facebookId, id))
  onLogIn = List(ExtendedSession.userDidLogin(_))
  onLogOut = List(ExtendedSession.userDidLogout(_))
}

class User extends MegaProtoUser[User] {
  def getSingleton = User
  object fullName extends MappedString(this, 128)
  object middleName extends MappedString(this, 64)
  object gender extends MappedString(this, 8)

  object facebookId extends MappedString(this, 16) {
    override def dbIndexed_? = true
  }
  object fbUserName extends MappedString(this, 64)
  object fbAccessToken extends MappedString(this, 256)
  object fbAccessTokenExpires extends MappedDateTime(this)
  object fbTimeZone extends MappedInt(this)

  object points extends MappedInt(this)

  def getFbProfilePictureUrl = "https://graph.facebook.com/%s/picture".format(facebookId)
}

object ExtendedSession extends ExtendedSession with MetaProtoExtendedSession[ExtendedSession] with Logger {
  override def dbTableName = "ext_session"

  def logUserIdIn(uid: String): Unit = User.logUserIdIn(uid)

  def recoverUserId: Box[String] = User.currentUserId

  type UserType = User
}

class ExtendedSession extends ProtoExtendedSession[ExtendedSession] {
  def getSingleton = ExtendedSession
}
