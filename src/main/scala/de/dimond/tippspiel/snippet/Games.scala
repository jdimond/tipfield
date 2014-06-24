package de.dimond.tippspiel.snippet

import org.scala_tools.time.Imports._
import de.dimond.tippspiel.model.PersistanceConfiguration._
import de.dimond.tippspiel.model.{ Game, Tip, Result }
import de.dimond.tippspiel.util._
import net.liftweb.common._
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.S
import net.liftweb.util.Helpers._
import net.liftweb.util._
import de.dimond.tippspiel.model.FacebookPool

class Games {

  val game = S.param("gameId") match {
    case Full(Long(gameId)) => Game.forId(gameId) match {
      case Some(g) => g
      case None => S.redirectTo("/")
    }
    case _ => S.redirectTo("/")
  }

  val users = (S.param("showAdmin"), User.currentUser) match {
    case (Full("true"), Full(user)) if user.isAdmin => User.findAll()
    case (_, Full(user)) => User.findAll(user.poolFriends + user.id)
    case _ => Seq()
  }

  val result = Result.forGame(game).toOption

  def gameRanking = {
    val ranking = User.rankUsers(users)
    ".ranking_entry" #> {
      ranking.map {
        case (rank, user) => {
          ".ranking_rank *" #> rank.is &
          ".ranking_full_name *" #> <a href={ "/tips/%s".format(user.fbId) }>{ user.fullName }</a> &
          "img [src]" #> user.profilePictureUrl &
          "#game_tip" #> TipForm.render(game, Tip.forUserAndGame(user, game), result, Some(user)) &
          ".ranking_points *" #> user.points
        }
      }
    }
  }

  def gameHeader = {
    "#game_team_home" #> SnippetHelpers.teamHtml(game.teamHome).reverse &
      "#game_team_away" #> SnippetHelpers.teamHtml(game.teamAway) &
      "#game_info *" #> DateHelpers.formatTime(game.date) &
      "#game_result *" #> (result match {
        case Some(result) => "%d : %d".format(result.goalsHome, result.goalsAway)
        case None => "- : -"
      })
  }
}
