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

import java.util.Date

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import de.dimond.tippspiel.model._


object Util {
  import scala.xml.transform._
  import scala.xml._
  val format = DateTimeFormat.forPattern("dd.MM.yy HH:mm");
  implicit def date2Joda(date: Date): DateTime = new DateTime(date)
  def formatTime(dateTime: DateTime) = format.print(dateTime)
  def uniquifyIds(ids: String => String)(nodeSeq: NodeSeq): NodeSeq = {
    val rr = new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case elem: Elem => {
          elem.attribute("id") match {
            case Some(Seq(Text(x))) => elem % new UnprefixedAttribute("id", Text(ids(x)), Null) toSeq
            case None => elem
          }
        }
        case other => other
      }
    }
    val transformer = new RuleTransformer(rr)
    nodeSeq map { transformer(_) }
  }
}

object TipForm {
  import scala.xml.Text
  import net.liftweb.http.js._
  import net.liftweb.http.js.jquery._
  import net.liftweb.http._
  def render(game: Game) = {
    var guessHome = 0
    var guessAway = 0

    val user = User.currentUser.open_!

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

    def process(): JsCmd = {
      if (guessHome >= 0 && guessAway >= 0) {
        val tip = Tip findByGame(user, game) openOr Tip.create.user(user).gameId(game.id)
        tip.goalsHome(guessHome)
        tip.goalsAway(guessAway)
        if (tip.save) {
          showResultImg("/images/check.png")
        } else {
          showResultImg("/images/fail.png")
        }
      } else {
        showResultImg("/images/fail.png")
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
    "form" #> {body => Util.uniquifyIds(mkId)(SHtml.ajaxForm(bodyTrans(body), showLoader))}
  }
}

class GameListing {
  import scala.xml.Text
  def teamHtml(ref: TeamReference) = ref.team match {
    case Left(str) => Text(str)
    case Right(team) => {
      Seq(<img src={"/images/flags/" + team.emblemUrl} />, Text(team.name))
    }
  }
  def list = "#games" #> { ".game" #> Game.all.map(game =>
    "#gameTime *" #> Util.formatTime(game.date) &
    "#gameTeamHome *" #> teamHtml(game.teamHome).reverse &
    "#gameTeamAway *" #> teamHtml(game.teamAway) &
    "#gameResult *" #> Result.goalsForGame(game) &
    "#gameTip" #> TipForm.render(game)
  )}
}
