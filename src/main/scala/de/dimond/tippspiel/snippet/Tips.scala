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

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.util._

object TipForm {
  import scala.xml._
  import net.liftweb.http.js._
  import net.liftweb.http.js.jquery._
  import net.liftweb.http._
  def render(game: Game): NodeSeq => NodeSeq = {
    def renderBeforeGame(user: User) = {
      var guessHome = 0
      var guessAway = 0

      def mkId(s: String) = {
        val ids = Set("gameAjaxLoader",
                      "saveGameTip")
        if (ids.contains(s)) {
          s + game.id
        } else {
          s
        }
      }

      def showResultImg(path: String) = {
        JqJsCmds.Hide(mkId("gameAjaxLoader")) &
        JqJE.Jq("#" + mkId("saveGameTip")) ~> JqJE.JqAttr("src", path) &
        JqJsCmds.Show(mkId("saveGameTip"))
      }

      def success = {
        showResultImg("/images/check.png")
      }

      def failure = {
        showResultImg("/images/fail.png")
      }

      def process(): JsCmd = {
        if (guessHome >= 0 && guessAway >= 0 && DateTime.now < game.date) {
          val tip = Tip findByGame(user, game) openOr Tip.create.user(user).gameId(game.id)
          tip.submissionTime(DateTime.now.toDate)
          tip.goalsHome(guessHome)
          tip.goalsAway(guessAway)
          if (tip.save) {
            success
          } else {
            failure
          }
        } else {
          failure
        }
      }

      val bodyTrans = {
        val tip = Tip findByGame(user, game)
        val goalsHome = tip map { _.goalsHome.is } openOr 0
        val goalsAway = tip map { _.goalsAway.is } openOr 0
        val resetOnKeyDown = BasicElemAttr("oninput", showResultImg("/images/go.png"))
        "#saveGameTip [src]" #> (if (tip.isEmpty) "/images/go.png" else "/images/check.png") &
        "name=guessHome" #> SHtml.number(goalsHome, guessHome = _, 0, 10, resetOnKeyDown) &
        "name=guessAway" #> SHtml.number(goalsAway, guessAway = _, 0, 10, resetOnKeyDown) &
        "name=process" #> SHtml.hidden(process)
      }
      val showLoader = JqJsCmds.Hide(mkId("saveGameTip")) & JqJsCmds.Show(mkId("gameAjaxLoader"))
      val hideLoader = JqJsCmds.Hide(mkId("gameAjaxLoader"))
      "form" #> {body => XmlHelpers.uniquifyIds(mkId)(SHtml.ajaxForm(bodyTrans(body), showLoader))}
    }

    def renderAfterGame(user: User) = "* *" #> {
      (Tip.findByGame(user, game), Result.forGame(game)) match {
        case (Full(tip), Full(result)) => {
          Text("%s : %s".format(tip.goalsHome.is, tip.goalsAway.is))
        }
        case (Full(tip), _) => {
          Text("%s : %s".format(tip.goalsHome.is, tip.goalsAway.is))
        }
        case _ => {
          Text("- : -")
        }
      }
    }

    def renderUserError = {
      _: NodeSeq => Text("")
    }

    User.currentUser match {
      case Full(user) => {
        if (DateTime.now < game.date) {
          renderBeforeGame(user)
        } else {
          renderAfterGame(user)
        }
      }
      case _ => renderUserError
    }
  }
}

object GameSnippet {
  import scala.xml.Text
  def teamHtml(ref: TeamReference) = ref.team match {
    case Left(str) => Text(str)
    case Right(team) => {
      Seq(<img src={"/images/flags/" + team.emblemUrl} />, Text(team.name))
    }
  }
  def html(game: Game) = {
    "#gameId *" #> game.id &
    "#gameTime *" #> DateHelpers.formatTime(game.date) &
    "#gameTeamHome *" #> teamHtml(game.teamHome).reverse &
    "#gameTeamAway *" #> teamHtml(game.teamAway) &
    "#gameResult *" #> Result.goalsForGame(game) &
    "#gameTip" #> TipForm.render(game)
  }
}

class GameListing {
  def list = "#games" #> { ".game" #> Game.all.map(GameSnippet.html(_)) }
}
