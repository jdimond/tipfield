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

object Standings {
  private object currentGroup extends RequestVar[String]("A")

  private val groupsToSelect = Group.all map { x => (x.name, "Group %s".format(x.name)) } toList
}

class Standings {
  import scala.xml.Text

  def teamHtml(team: Team) = Seq(<img src={"/images/flags/" + team.emblemUrl} />, Text(team.name))

  def standings = {
    import Standings._
    val tableHtml = SHtml.idMemoize( table => {
      ".standing" #> Group.forName(currentGroup.is).standings.zipWithIndex.map(x => {
        val standing = x._1
        val i = x._2 + 1
        "#standing_rank *" #> i &
        "#standing_team *" #> teamHtml(standing.team) &
        "#standing_played *" #> standing.gamesPlayed &
        "#standing_won *" #> standing.won &
        "#standing_drawn *" #> standing.drawn &
        "#standing_lost *" #> standing.lost &
        "#standing_goals *" #> "%s : %s".format(standing.goalsScored, standing.goalsReceived) &
        "#standing_points *" #> standing.points
      })
    })
    "#group_select" #> ajaxSelect(groupsToSelect, Full(currentGroup.is), s => { currentGroup(s); tableHtml.setHtml }) &
    "#standings" #> tableHtml
  }

}
