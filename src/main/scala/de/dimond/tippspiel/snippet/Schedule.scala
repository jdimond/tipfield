package de.dimond.tippspiel
package snippet

//import scala.xml.{NodeSeq, Text}
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import Helpers._
import net.liftweb.mapper.By

import java.util.Date

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import de.dimond.tippspiel.model._

class Schedule {
  import scala.xml._

  def buildLinkNext(matchDay: Option[MatchDay]): NodeSeq = {
    matchDay match {
      case Some(m) => {
        <a href={"/schedule/%s".format(m.id)} class="btn">
          { m.toString() }
          <i class="icon-arrow-right"></i>
        </a>
      }
      case None => {
        <a href="#" class="btn disabled" style="width: 70px">
          <i class="icon-arrow-right"></i>
        </a>
      }
    }
  }

  def buildLinkPrev(matchDay: Option[MatchDay]): NodeSeq = {
    matchDay match {
      case Some(m) => {
        <a href={"/schedule/%s".format(m.id)} class="btn">
          <i class="icon-arrow-left"></i>
          { m.toString() }
        </a>
      }
      case None => {
        <a href="#" class="btn disabled" style="width: 70px">
          <i class="icon-arrow-left"></i>
        </a>
      }
    }
  }

  def schedule = {
    val matchdayId = S.param("matchday") openOr MatchDay.all.head.id
    val matchday = MatchDay.forId(matchdayId)
    val all = MatchDay.all
    "#matchday_select" #> {
      "#matchday_name" #> matchday.toString() &
      "#matchday_items" #> {
        "li *" #> all.map(x => <a href={"/schedule/%s".format(x.id)}>{x.toString()}</a>)
      }
    } &
    "#next_matchday *" #> (all.indexOf(matchday) match {
      case x if x == all.size - 1 => buildLinkNext(None)
      case x => buildLinkNext(Some(all(x + 1)))
    }) &
    "#prev_matchday *" #> (all.indexOf(matchday) match {
      case x if x == 0 => buildLinkPrev(None)
      case x => buildLinkPrev(Some(all(x - 1)))
    }) &
    "#games" #> { ".game" #> matchday.games.map(GameSnippet.html(_)) }
  }
}
