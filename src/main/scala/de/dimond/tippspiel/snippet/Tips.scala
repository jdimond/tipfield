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
import de.dimond.tippspiel.model.{Game, Tip, Trivia, Result, User}
import de.dimond.tippspiel.model.PersistanceConfiguration._
import de.dimond.tippspiel.util._

object TipForm extends Logger {
  import scala.xml._
  import net.liftweb.http.js._
  import net.liftweb.http.js.jquery._
  import net.liftweb.http._
  def render(game: Game, tip: Option[Tip], result: Option[Result], user: Option[User]): NodeSeq => NodeSeq = {
    val isCurrentUser = (User.currentUser, user) match {
      case (Full(curUser), Some(user)) if (curUser.id == user.id) => true
      case _ => false
    }
    def renderBeforeGame(user: model.User) = {
      val popoverId = nextFuncName
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
              /* Don't let the trivia disturb the saving of the user tips, so catch all exceptions */
              try {
                Trivia.generate(game, user, h, a) match {
                  case Some(trivia) => {
                    if (trivia.startsWith("trivia_")) {
                      warn("Translation not found for: %s".format(trivia))
                      success
                    } else {
                      val encTrivia = encJs(trivia)
                      val encId = encJs(popoverId)
                      Run("$('#' + %s).popover({content:%s})".format(encId, encTrivia)) &
                      Run("$('#' + %s).data('popover').options.content= %s;".format(encId, encTrivia)) &
                      Run("$('#' + %s).popover('show');".format(encId)) &
                      success
                    }
                  }
                  case None => success
                }
              } catch {
                case e => {
                  warn("Error during trivia generation", e)
                  success
                }
              }
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
      "form" #> {body => XmlHelpers.uniquifyIds(mkId)(SHtml.ajaxForm(bodyTrans(body), showLoader))} &
      "#game_tip [id]" #> popoverId
    }

    def renderAfterGame(user: model.User) = "* *" #> {
      def renderValue(value: String) = {
        <input disabled="disabled" class="input goal_input disabled_tip" value={ value } />
      }
      (tip, result) match {
        case (Some(tip), Some(result)) => {
          renderValue(tip.goalsHome.toString) ++ Text(" : ") ++ renderValue(tip.goalsAway.toString) ++
          Text(" ") ++
          (tip.points match {
            case Some(PointsExact) => <span class="badge badge-success">{ PointsExact.points }</span>
            case Some(PointsSameDifference) => <span class="badge badge-info">{ PointsSameDifference.points }</span>
            case Some(PointsTendency) => <span class="badge badge-warning">{ PointsTendency.points }</span>
            case Some(PointsNone) => <span class="badge badge-important">{ PointsNone.points }</span>
            case _ => <span class="badge">&nbsp;&nbsp;</span>
          })
        }
        case (Some(tip), _) => {
          renderValue(tip.goalsHome.toString) ++ Text(" : ") ++ renderValue(tip.goalsAway.toString) ++
          Text(" ") ++ <span class="badge">&nbsp;&nbsp;</span>
        }
        case _ => {
          renderValue("-") ++ Text(" : ") ++ renderValue("-") ++
          Text(" ") ++ <span class="badge">&nbsp;&nbsp;</span>
        }
      }
    }

    def renderHidden = "* *" #> {
      <input disabled="disabled" class="input goal_input disabled_tip" value="?" /> ++ Text(" : ") ++
      <input disabled="disabled" class="input goal_input disabled_tip" value="?" /> ++
      Text(" ") ++ <span class="badge">&nbsp;&nbsp;</span>
    }

    def renderUserError = {
      _: NodeSeq => Text("")
    }

    def renderLogin = "* *" #> (<a href="/facebook/authorize" class="btn btn-primary">Login</a>)

    user match {
      case Some(user) => {
        if (DateTime.now < game.date) {
          if (isCurrentUser) {
            renderBeforeGame(user)
          } else {
            renderHidden
          }
        } else {
          renderAfterGame(user)
        }
      }
      case None => renderLogin
    }
  }
}

object GameSnippet extends Logger {
  import scala.xml.Text
  private def row(game: Game, tip: Option[Tip], result: Option[Result], user: Option[User]) = {
    "#game_id *" #> game.id &
    "#game_time *" #> DateHelpers.formatTime(game.date) &
    "#game_team_home *" #> SnippetHelpers.teamHtml(game.teamHome).reverse &
    "#game_team_away *" #> SnippetHelpers.teamHtml(game.teamAway) &
    "#game_result *" #> (result match {
      case Some(result) => "%d : %d".format(result.goalsHome, result.goalsAway)
      case None => "- : -"
    }) &
    "#game_tip" #> TipForm.render(game, tip, result, user)
  }

  def render(games: Seq[Game], user: Option[User]) = {
    val tips: Map[Game, Tip] = user match {
      case Some(user) => Tip.forUserAndGames(user, games)
      case None => Map()
    }
    ".game" #> games.map(game => row(game, tips.get(game), Result.forGame(game).toOption, user))
  }
}
