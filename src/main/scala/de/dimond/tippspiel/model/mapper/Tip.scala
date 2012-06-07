package de.dimond.tippspiel.model.mapper

import de.dimond.tippspiel.model._

import net.liftweb.mapper._
import net.liftweb.common._

import org.scala_tools.time.Imports._
import java.util.Date

object DbTip extends DbTip with LongKeyedMetaMapper[DbTip] with MetaTip {
  def updatePoints(result: Result): Boolean = {
    val all = findAll(By(_gameId, result.gameId))
    val saved = for {
      tip <- all
      val points = Points.apply(tip.goalsHome, tip.goalsAway, result.goalsHome, result.goalsAway)
      val x = tip._points(Full(points.points.toLong))
    } yield tip.save()
    return saved.foldLeft(true)(_ && _)
  }

  def forUserAndGame(user: User, game: Game): Box[Tip] = find(By(_userId, user.id), By(_gameId, game.id))
  def forUserAndGames(user: User, games: Seq[Game]): Map[Game, Tip] = {
    val tips = findAll(By(_userId, user.id), ByList(_gameId, games.map(_.id)))
    val tipMap = tips.map(tip => (tip.gameId, tip)).toMap
    val gameTipSeq = for {
      game <- games
      tip <- tipMap.get(game.id)
    } yield (game -> tip)
    gameTipSeq.toMap
  }

  def forUsersAndGame(userIds: Set[Long], game: Game): Map[Long, Tip] = {
    val tips = findAll(ByList(_userId, userIds.toSeq), By(_gameId, game.id))
    tips.map(tip => (tip.userId, tip)).toMap
  }

  def saveForUserAndGame(user: User, game: Game, goalsHome: Int, goalsAway: Int): Boolean = {
    if (DateTime.now > game.date) {
      return false
    }
    val tip = find(By(_userId, user.id), By(_gameId, game.id)) openOr DbTip.create._userId(user.id)._gameId(game.id)
    tip._goalsHome(goalsHome)
    tip._goalsAway(goalsAway)
    tip._submissionTime(new Date())
    return tip.save()
  }

  def statsForGame(game: Game): Option[TipStats] = {
    object gameStats extends TipStats {
      var totalTips: Long = 0
      var tipsHome = new Array[Long](10)
      var tipsAway = new Array[Long](10)
      var tips = Array.ofDim[Long](10, 10)

      override def numberOfTipsWhere(goalsHome: Option[Int], goalsAway: Option[Int]) = {
        (goalsHome, goalsAway) match {
          case (Some(gh), Some(ga)) => tips(gh)(ga)
          case (Some(gh), None) => tipsHome(gh)
          case (None, Some(ga)) => tipsAway(ga)
          case (None, None) => totalTips
        }
      }

      var totalGoalsHome: Long = 0
      var totalGoalsAway: Long = 0
      override def averageGoalsHome = 1.0*totalGoalsHome/totalTips
      override def averageGoalsAway = 1.0*totalGoalsAway/totalTips

      var numberOfAwayWins: Long = 0
      var numberOfHomeWins: Long = 0
      var numberOfDraws: Long = 0
    }
    for (tip <- findAllFields(Seq(_goalsHome, _goalsAway), By(_gameId, game.id))) {
      val gh = tip._goalsHome
      val ga = tip._goalsAway
      gameStats.totalTips += 1
      gameStats.tipsHome(gh) += 1
      gameStats.tipsAway(ga) += 1
      gameStats.tips(gh)(ga) += 1
      if (gh > ga) {
        gameStats.numberOfHomeWins += 1
      } else if (ga > gh) {
        gameStats.numberOfAwayWins += 1
      } else {
        gameStats.numberOfDraws += 1
      }
      gameStats.totalGoalsHome += gh
      gameStats.totalGoalsAway += ga
    }
    Some(gameStats)
  }
}

class DbTip extends Tip with LongKeyedMapper[DbTip] with IdPK {
  def getSingleton = DbTip

  protected object _userId extends MappedLong(this) {
    override def dbIndexed_? = true
  }
  protected object _gameId extends MappedLong(this) {
    override def dbIndexed_? = true
  }
  protected object _goalsHome extends MappedInt(this)
  protected object _goalsAway extends MappedInt(this)
  protected object _points extends MappedNullableLong(this)
  protected object _submissionTime extends MappedDateTime(this)

  def userId = _userId.is
  def gameId = _gameId.is
  def goalsHome = _goalsHome.is
  def goalsAway = _goalsAway.is
  def points = _points.is.flatMap(i => Points(i.toInt)).toOption
  def submissionTime = new DateTime(_submissionTime.is)
}
