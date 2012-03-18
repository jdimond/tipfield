package de.dimond.tippspiel.model

import org.joda.time.DateTime

import net.liftweb.mapper._

case class Standing(team: Team, gamesPlayed: Int, won: Int, drawn: Int, lost: Int, goalsScored: Int,
                    goalsReceived: Int, points: Int) {
  def +(s: Standing) = {
    if (s.team != team) {
      throw new IllegalArgumentException("Teams do not match: %s != %s".format(team, s.team))
    }
    Standing(team, gamesPlayed + s.gamesPlayed, won + s.won, drawn + s.drawn, lost + s.lost,
             goalsScored + s.goalsScored, goalsReceived + s.goalsReceived, points + s.points)
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

  def standings = {
    val results = games.map(game => ((game, Result.forGame(game))))
    val standings = for { resultBox <- results; result <- resultBox._2; game = resultBox._1 } yield {
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
    (initialStandings ++ standings.flatten).groupBy(_.team).values.map(_.reduce(_ + _)).toList.sortWith((x, y) => {
      val diffX = x.goalsScored - x.goalsReceived
      val diffY = y.goalsScored - y.goalsReceived
      x.points > y.points || (x.points == y.points && diffX > diffY) ||
      (x.points == y.points && diffX == diffY && x.goalsScored > y.goalsScored)
    })
  }
}

case class Matchday(name: String, games: Seq[Game])

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
  override def team = Left("Winner Group " + group.name)
}
case class GroupRunnerUp(group: Group) extends TeamReference {
  override def team = Left("Runner-up Group " + group.name)
}
case class GameWinner(game: Game) extends TeamReference {
  override def team = Left("Winner Match " + game.id)
}

object Game {
  def all = games.values.toList.sortWith((e1, e2) => (e1.date compareTo e2.date) < 0)
  private var games: Map[Int, Game] = Map()
}

case class Location(location: String)

case class Game(id: Int, teamHome: TeamReference, teamAway: TeamReference, date: DateTime, location: Location) {
  Game.games = Game.games + ((id, this))
}

case class Team(name: String, emblemUrl: String) {
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
  def goalsForGame(game: Game) = find(By(Result.gameId, game.id)) map { _.goals } openOr ""
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
