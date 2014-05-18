package de.dimond.tippspiel.model

import scala.actors.Actor
import net.liftweb.common._

import org.scala_tools.time.Imports._

trait MetaTip {
  def updatePoints(result: Result): Boolean
  def forUserAndGame(user: User, game: Game): Box[Tip]
  def forUserAndGames(user: User, games: Seq[Game]): Map[Game, Tip]
  def forUsersAndGame(userIds: Set[Long], game: Game): Map[Long, Tip]
  def numberPlacedForUser(user: User): Int
  def saveForUserAndGame(user: User, game: Game, goalsHome: Int, goalsAway: Int): Boolean
  def statsForGame(game: Game): Option[TipStats]
}

trait TipStats {
  def numberOfTipsWhere(goalsHome: Option[Int] = None, goalsAway: Option[Int] = None): Long
  def averageGoalsHome: Double
  def averageGoalsAway: Double
  def numberOfDraws: Long
  def numberOfHomeWins: Long
  def numberOfAwayWins: Long
}

object TipStatsUpdater extends Actor with Logger {
  case object Update

  val updateInterval = 15 minutes

  private var lastUpdate = DateTime.now - updateInterval
  private var map: Map[Game, TipStats] = Map()

  def statsForGame(game: Game) = {
    if (lastUpdate + updateInterval < DateTime.now) {
      TipStatsUpdater ! Update
    }
    map.get(game)
  }

  def act = loop {
    import PersistanceConfiguration._
    react {
      case Update => {
        if (lastUpdate + updateInterval < DateTime.now) {
          info("Updating game stats")
          val all = for {
            game <- Game.all
            stats <- Tip.statsForGame(game)
          } yield (game -> stats)
          map = all.toMap
          lastUpdate = DateTime.now
        } else {
          info("Stats are up to date, not updating!")
        }
      }
    }
  }

  this.start
}

object TipCountManager extends Actor with Logger {
  case class SetDirty(user: User)

  def act = loop {
    import PersistanceConfiguration._
    react {
      case SetDirty(user) => {
        info("Updating tip counts for user %d".format(user.id))
        user.numberOfTips = Tip.numberPlacedForUser(user)
        user.numberOfSpecials = SpecialTip.numberPlacedForUser(user)
        user.save()
      }
    }
  }

  this.start
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
  def apply(i: Int): Option[Points] = i match {
    case PointsExact.points => Some(PointsExact)
    case PointsSameDifference.points => Some(PointsSameDifference)
    case PointsTendency.points => Some(PointsTendency)
    case PointsNone.points => Some(PointsNone)
    case _ => None
  }
}

sealed trait Points {
  def points: Int
}
case object PointsExact extends Points {
  val points = 3
}
case object PointsSameDifference extends Points {
  val points = 2
}
case object PointsTendency extends Points {
  val points = 1
}
case object PointsNone extends Points {
  val points = 0
}
