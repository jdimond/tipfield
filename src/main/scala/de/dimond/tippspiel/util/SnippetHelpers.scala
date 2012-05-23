package de.dimond.tippspiel.util

import de.dimond.tippspiel._
import model._
import scala.xml._
import net.liftweb.http._
import net.liftweb.common._

object SnippetHelpers extends Logger {
  def teamHtml(ref: TeamReference) = ref.team match {
    case Left((str, id)) => Text(S.?(str).format(id))
    case Right(team) => {
      Seq(<img src={"/images/flags/" + team.emblemUrl} />, Text(team.localizedName))
    }
  }

  def die(text: String) = {
    warn(text)
    Full(PlainTextResponse(text, Nil, 400))
  }

  def replaceNewlinesWithBrs(s: String): NodeSeq = {
    s.split("\n").toList.flatMap(x => Text(x) ++ <br />).dropRight(1)
  }
}
