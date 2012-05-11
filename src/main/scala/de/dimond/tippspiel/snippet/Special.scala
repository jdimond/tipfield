package de.dimond.tippspiel
package snippet

//import scala.xml.{NodeSeq, Text}
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http.SHtml
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.jquery._
import net.liftweb.http.js.JE._
import Helpers._
import net.liftweb.mapper.By

import org.scala_tools.time.Imports._

import de.dimond.tippspiel._
import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._

object SpecialSnippet {
  def html(user: User, special: Special) = {
    val ajaxId = nextFuncName
    val resultId = nextFuncName
    val tip = SpecialTip.answerForUser(user, special)
    val answers = (if (tip.isEmpty) Seq(("", "")) else Seq()) ++
      special.answers.zipWithIndex.map(t => (t._2.toString, t._1.localizedAnswer))
    val selected = tip.map(_.answerId.toString) or Full("")
    val showAjaxLoader = Call("toggleShowing", resultId, ajaxId)

    def showResultImg(path: String) = {
      JqJsCmds.Hide(ajaxId) &
      JqJE.JqId(resultId) ~> JqJE.JqAttr("src", path) &
      JqJsCmds.Show(resultId)
    }
    def success = showResultImg("/images/check.png")
    def failure = showResultImg("/images/fail.png")

    def saveTip(idStr: String) = {
      try {
        val id = idStr.toInt
        if (id >= 0 && id < special.answers.length && DateTime.now < special.finalAnswerTime) {
          if (SpecialTip.saveForUser(user, special, id)) {
            success
          } else {
            failure
          }
        } else {
          failure
        }
      } catch {
        case _: NumberFormatException => failure
      }
    }

    ".special_title *" #> special.localizedTitle &
    ".special_select" #> SHtml.ajaxSelect(answers, selected, showAjaxLoader, saveTip _) &
    "#special_button [src]" #> (if (tip.isEmpty) "/images/fail.png" else "/images/check.png") &
    "#special_ajax_loader [id]" #> ajaxId &
    "#special_button [id]" #> resultId
  }
}

class TipOverview {
  val user = User.currentUser.open_!

  def listSpecials = ".special_question" #> Special.all.map(SpecialSnippet.html(user, _))
  def listGames = "#games" #> { ".game" #> Game.all.map(GameSnippet.html(_)) }
}
