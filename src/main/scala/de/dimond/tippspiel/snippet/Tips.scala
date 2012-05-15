package de.dimond.tippspiel
package snippet

//import scala.xml.{NodeSeq, Text}
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http.SHtml
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import Helpers._
import net.liftweb.mapper.By

import org.scala_tools.time.Imports._

import de.dimond.tippspiel._
import de.dimond.tippspiel.model.{Points, PointsExact, PointsTendency, PointsSameDifference, PointsNone}
import de.dimond.tippspiel.model.{Game, Tip}
import de.dimond.tippspiel.model.PersistanceConfiguration._
import de.dimond.tippspiel.util._

object TipForm {
  import scala.xml._
  import net.liftweb.http.js._
  import net.liftweb.http.js.jquery._
  import net.liftweb.http._
  def render(game: Game, tip: Option[Tip]): NodeSeq => NodeSeq = {
    def renderBeforeGame(user: model.User) = {
      var guessHome = ""
      var guessAway = ""

      def mkId(s: String) = {
        val ids = Set("game_ajax_loader",
                      "save_game_button")
        if (ids.contains(s)) {
          s + game.id
        } else {
          s
        }
      }

      def showResultImg(path: String) = {
        JqJsCmds.Hide(mkId("game_ajax_loader")) &
        JqJE.JqId(mkId("save_game_button")) ~> JqJE.JqAttr("src", path) &
        JqJsCmds.Show(mkId("save_game_button"))
      }

      def success = {
        showResultImg("/images/check.png")
      }

      def failure = {
        showResultImg("/images/fail.png")
      }

      def process(): JsCmd = {
        val home = Util.parseInt(guessHome)
        val away = Util.parseInt(guessAway)
        (home, away) match {
          case (Some(h), Some(a)) if (h >= 0 && a >= 0 && DateTime.now < game.date) => {
            val saved = Tip.saveForUserAndGame(user, game, h, a)
            if (saved) {
              success
            } else {
              failure
            }
          }
          case _ => failure
        }
      }

      val bodyTrans = {
        val goalsHome = tip map { _.goalsHome.toString } getOrElse "0"
        val goalsAway = tip map { _.goalsAway.toString } getOrElse "0"
        "#save_game_button [src]" #> (if (tip.isEmpty) "/images/fail.png" else "/images/check.png") &
        "name=guess_home" #> SHtml.text(goalsHome, guessHome = _) &
        "name=guess_away" #> SHtml.text(goalsAway, guessAway = _) &
        "name=process" #> SHtml.hidden(process)
      }
      val showLoader = JqJsCmds.Hide(mkId("save_game_button")) & JqJsCmds.Show(mkId("game_ajax_loader"))
      val hideLoader = JqJsCmds.Hide(mkId("game_ajax_loader"))
      "form" #> {body => XmlHelpers.uniquifyIds(mkId)(SHtml.ajaxForm(bodyTrans(body), showLoader))}
    }

    def renderAfterGame(user: model.User) = "* *" #> {
      def renderValue(value: String) = {
        <input disabled="disabled" class="input goal_input disabled_tip" value={ value } />
      }
      (Tip.forUserAndGame(user, game), Result.forGame(game)) match {
        case (Full(tip), Full(result)) => {
          renderValue(tip.goalsHome.toString) ++ Text(" : ") ++ renderValue(tip.goalsAway.toString) ++
          Text(" ") ++
          (tip.points match {
            case Some(PointsExact) => <span class="badge badge-success">{ PointsExact.points }</span>
            case Some(PointsSameDifference) => <span class="badge badge-warning">{ PointsSameDifference.points }</span>
            case Some(PointsTendency) => <span class="badge badge-info">{ PointsTendency.points }</span>
            case Some(PointsNone) => <span class="badge badge-error">{ PointsNone.points }</span>
            case _ => <span class="badge">&nbsp;&nbsp;</span>
          })
        }
        case (Full(tip), _) => {
          renderValue(tip.goalsHome.toString) ++ Text(" : ") ++ renderValue(tip.goalsAway.toString)
        }
        case _ => {
          renderValue("-") ++ Text(" : ") ++ renderValue("-") ++
          Text(" ") ++ <span class="badge">&nbsp;&nbsp;</span>
        }
      }
    }

    def renderUserError = {
      _: NodeSeq => Text("")
    }

    def renderLogin = "* *" #> (<a href="/facebook/authorize" class="btn btn-primary">Login</a>)

    User.currentUser match {
      case Full(user) => {
        if (DateTime.now < game.date) {
          renderBeforeGame(user)
        } else {
          renderAfterGame(user)
        }
      }
      case _ => renderLogin
    }
  }
}

object GameSnippet {
  import scala.xml.Text
  private def row(game: Game, tip: Option[Tip], hidden: Boolean = false) = {
    "tr [id]" #> "game_id%d".format(game.id) &
    (if (hidden) "tr [style]" #> "display: none"
     else "tr [style]" #> "") &
    "#game_id *" #> game.id &
    "#game_time *" #> DateHelpers.formatTime(game.date) &
    "#game_team_home *" #> SnippetHelpers.teamHtml(game.teamHome).reverse &
    "#game_team_away *" #> SnippetHelpers.teamHtml(game.teamAway) &
    "#game_result *" #> (Result.forGame(game) match {
      case Full(result) => "%d : %d".format(result.goalsHome, result.goalsAway)
      case _ => "- : -"
    }) &
    "#game_tip" #> TipForm.render(game, tip)
  }

  def render(games: Seq[Game]) = {
    val tips: Map[Game, Tip] = User.currentUser match {
      case Full(user) => Tip.forUserAndGames(user, games)
      case _ => Map()
    }
    ".game" #> games.map(game => row(game, tips.get(game)))
  }
}

/*
class GameListing {
  val user = User.currentUser open_!
  def list = "#games" #> { ".game" #> Game.all.filter(game => {
      DateTime.now < game.date &&
      game.teamAway.team.isRight &&
      game.teamHome.team.isRight &&
      Tip.forUserAndGame(user, game).isEmpty
    }).zipWithIndex.map(g => GameSnippet.html(g._1, g._2 >= 3)) }
}
*/
