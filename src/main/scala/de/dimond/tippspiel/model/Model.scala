package de.dimond.tippspiel.model

import org.joda.time.DateTime

import net.liftweb.mapper._

import Group.Group

object Group extends Enumeration {
  type Group = Value
  val A = Value("A")
  val B = Value("B")
  val C = Value("C")
  val D = Value("D")
}

object Matchday extends Enumeration {
  val One = Value("1. Spieltag")
  val Two = Value("2. Spieltag")
  val Three = Value("3. Spieltag")
  val quarterfinals = Value("Viertelfinale")
  val semifinals = Value("Halbfinale")
  val final_ = Value("Finale")
}

sealed trait TeamReference {
  def teamAvailable: Boolean
  def getTeam: Either[String, Team]
}

case class DirectTeamReference(team: Team) extends TeamReference {
  override def teamAvailable = true
  override def getTeam = Right(team)
}
case class GroupWinnerReference(group: Group) extends TeamReference {
  override def teamAvailable = false
  override def getTeam = Left("Group Winner")
}
case class GroupSecondReference(group: Group) extends TeamReference {
  override def teamAvailable = false
  override def getTeam = Left("Group Runner-up")
}

case class MatchWinnerReference(game: Game) extends TeamReference {
  override def teamAvailable = false
  override def getTeam = Left("Group Runner-up")
}

object Tip extends Tip with LongKeyedMetaMapper[Tip] {
  def findByGame(user: User, game: Game) = {
    Tip find (By(Tip.user, user), By(Tip.game, game))
  }
}

class Tip extends LongKeyedMapper[Tip] with IdPK {
  def getSingleton = Tip
  object user extends MappedLongForeignKey(this, User)
  object game extends MappedLongForeignKey(this, Game)
  object goalsHome extends MappedInt(this)
  object goalsAway extends MappedInt(this)
}

object Result extends Result with LongKeyedMetaMapper[Result] {
  def goalsForGame(game: Game) = find(By(Result.game, game)) map { _.goals } openOr ""
}

class Result extends LongKeyedMapper[Result] with IdPK {
  def getSingleton = Result
  object game extends MappedLongForeignKey(this, Game)
  object goalsHome extends MappedInt(this)
  object goalsAway extends MappedInt(this)

  def goals = goalsHome.is + " : " + goalsAway.is
}

object Game extends Game with LongKeyedMetaMapper[Game] {
  def all = findAll(OrderBy(date, Ascending),
                    OrderBy(group, Ascending))
}

class Game extends LongKeyedMapper[Game] with IdPK {
  def getSingleton = Game
  object group extends MappedEnum(this, Group)
  object matchday extends MappedEnum(this, Matchday)
  object teamHome extends MappedLongForeignKey(this, Team)
  object teamAway extends MappedLongForeignKey(this, Team)
  object date extends MappedDateTime(this) {
    def asJoda = new DateTime(date.is)
  }
  object location extends MappedString(this, 100)
}

object Team extends Team with LongKeyedMetaMapper[Team];

class Team extends LongKeyedMapper[Team] with IdPK {
  def getSingleton = Team
  object name extends MappedString(this, 100)
  object emblemUrl extends MappedString(this, 256)
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
