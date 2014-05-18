package de.dimond.tippspiel
package snippet

import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js._
import net.liftweb.http.js.jquery._
import net.liftweb.util.Helpers._

import scala.xml._

import org.scala_tools.time.Imports._

import de.dimond.tippspiel.model.PersistanceConfiguration._
import de.dimond.tippspiel.model._
import de.dimond.tippspiel.util._
import DateHelpers.Implicits._

object Admin {
  private def redirect(js: JsCmd) = S.redirectTo("/admin", () => S.appendJs(js))
  sealed trait AlertMode
  case object Success extends AlertMode
  case object Error extends AlertMode
  def showAlert(mode: AlertMode, title: String, text: String) = {
    val func = mode match {
      case Success => "success"
      case Error => "error"
    }
    redirect(JE.Call(func, title, text))
  }
}

class Admin {
  import Admin._

  def specialRow(special: Special) = {
    var specialAnswer: String = ""
    val id = Helpers.nextFuncName
    def process(): JsCmd = {
      val start = System.currentTimeMillis
      SpecialResult.save(special, Util.parseInt(specialAnswer)) match {
        case Full(r) => {
          showAlert(Success, "Saved", "Operation took %dms.".format(System.currentTimeMillis - start))
        }
        case _ => {
          showAlert(Error, "Error", "Failed to save result and update points!")
        }
      }
    }
    val specialResult = SpecialResult.forSpecial(special)
    val answers = ("", "") :: special.answers.zipWithIndex.map(t => (t._2.toString, t._1.localizedAnswer)).toList
    val selected = specialResult.map(_.answerId.toString) orElse Some("-1")

    ".special_select" #> SHtml.select(answers, selected, specialAnswer = _) &
    "#special_question *" #> "%s (%d %s)".format(special.localizedTitle, special.points, S.?("points")) &
    "button [id]" #> id &
    "button [class+]" #> (if (specialResult.isEmpty) "" else "btn-success") &
    "name=process" #> SHtml.hidden(process)
  }

  def specialEditor = {
    ".special_entry" #> { Special.all.filter(DateTime.now > _.finalAnswerTime).sortBy(_.id).map(specialRow(_)) }
  }

  def resultForm(game: Game) = {
    var resultHome: String = ""
    var resultAway: String = ""
    val id = Helpers.nextFuncName
    def process(): JsCmd = {
      (Util.parseInt(resultHome), Util.parseInt(resultAway)) match {
        case (Some(home), Some(away)) if home >= 0 && away >= 0 => {
          val start = System.currentTimeMillis
          Result.saveForGame(game, home, away) match {
            case Full(r) => {
              showAlert(Success, "Saved", "Operation took %dms.".format(System.currentTimeMillis - start))
            }
            case _ => {
              showAlert(Error, "Error", "Failed to save result and update points!")
            }
          }
        }
        case _ => {
          showAlert(Error, "Error", "Malformed Goals!")
        }
      }
    }
    val bodyTrans = {
      val result = Result.forGame(game)
      val goalsHome = result map { _.goalsHome.toString } openOr ""
      val goalsAway = result map { _.goalsAway.toString } openOr ""
      "name=result_home" #> SHtml.text(goalsHome, resultHome = _) &
      "name=result_away" #> SHtml.text(goalsAway, resultAway = _) &
      "button [id]" #> id &
      "button [class+]" #> (if (result.isEmpty) "" else "btn-success") &
      "name=process" #> SHtml.hidden(process)
    }
    "#result_form" #> { body => bodyTrans(body) }
  }

  def resultEditor = {
    ".result_entry" #> { Game.all.filter(DateTime.now > _.date).sortBy(_.date).reverse.map(game => {
      "#game_time *" #> DateHelpers.formatTime(game.date) &
      "#game_team_home *" #> SnippetHelpers.teamHtml(game.teamHome).reverse &
      "#game_team_away *" #> SnippetHelpers.teamHtml(game.teamAway) &
      "#result_form" #> resultForm(game)
    })}
  }

  def recalculate() = {
    val now = System.currentTimeMillis
    def update(game: Game) = Result.forGame(game) match {
      case Full(r) => {
        Tip.updatePoints(r)
        true
      }
      case f: Failure => false
      case Empty => true
    }
    val allSuccess = for {
      game <- Game.all
      val s = update(game)
    } yield s
    val gamesSuccess = allSuccess.foldLeft(true)(_ && _)
    val totalSuccess = gamesSuccess && User.updatePointsAndRanking()
    if (totalSuccess) {
      showAlert(Success, "Success", "Operation took %dms.".format(System.currentTimeMillis - now))
    } else {
      showAlert(Error, "Failed", "Operation took %dms".format(System.currentTimeMillis - now))
    }
  }

  def databaseTools = {
    "#recalculate_all" #> {
      "name=process" #> SHtml.hidden(recalculate)
    }
  }

  def stats = {
    val registeredUsers = User.totalCount
    val totalTips = Tip.totalCount
    val totalSpecials = SpecialTip.totalCount
    "#registered_users" #> Text(registeredUsers.toString) &
    "#placed_tips" #> Text(totalTips.toString) &
    "#placed_specials" #> Text(totalSpecials.toString) &
    "#tips_per_user" #> Text((1.0*totalTips / registeredUsers).toString) &
    "#specials_per_user" #> Text((1.0*totalSpecials / registeredUsers).toString)
  }
}
