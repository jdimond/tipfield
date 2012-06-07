package de.dimond.tippspiel
package snippet

//import scala.xml.{NodeSeq, Text}
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http.{SHtml, S}
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

object SpecialSnippet extends Logger {
  def html(user: User, special: Special, tips: Map[Special, SpecialTip]) = {
    val tip = tips.get(special)
    val answers = (if (tip.isEmpty) Seq(("", "")) else Seq()) ++
      special.answers.zipWithIndex.map(t => (t._2.toString, t._1.localizedAnswer))

    def beforeDeadline = {
      val ajaxId = nextFuncName
      val resultId = nextFuncName
      val selected = tip.map(_.answerId.toString) orElse Some("")
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
      ".special_select" #> SHtml.ajaxSelect(answers, selected, showAjaxLoader, saveTip _) &
      "#special_button [src]" #> (if (tip.isEmpty) "/images/fail.png" else "/images/check.png") &
      "#special_ajax_loader [id]" #> ajaxId &
      "#special_button [id]" #> resultId
    }

    def afterDeadline = {
      val answer = tip.flatMap(tip =>
        if (tip.answerId >= 0 && tip.answerId < special.answers.size) {
          Some(special.answers(tip.answerId).localizedAnswer)
        } else {
          warn("Found answer with from id %d for special %s".format(special, tip.answerId))
          None
        }
      ) getOrElse "-"
      val specialResult = SpecialResult.forSpecial(special)
      val inputClass = (specialResult, tip) match {
        case (Full(r), Some(t)) if (r.answerId == t.answerId) => "special_select special_right"
        case (Full(r), Some(t)) if (r.answerId != t.answerId) => "special_select special_wrong"
        case _ => "special_select"
      }
      val answerHtml = <input disabled="disabled" class={ inputClass } value={ answer } />
      ".special_answer *" #> answerHtml
    }

    ".special_title *" #> special.localizedTitle &
    ".special_points *" #> special.points &
    ".special_answer" #> (if (DateTime.now < special.finalAnswerTime) beforeDeadline else afterDeadline)
  }
}

class TipOverview {
  val user = User.currentUser.open_!

  def listSpecials = {
    val tips = SpecialTip.answersForUser(user, Special.all)
    ".special_question" #> Special.all.map(SpecialSnippet.html(user, _, tips))
  }

  def listGames = "#games" #> GameSnippet.render(Game.all)
}
