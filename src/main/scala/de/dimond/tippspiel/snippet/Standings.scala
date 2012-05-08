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

class Standings {
  import scala.xml.Text

  def teamHtml(team: Team) = Seq(<img src={"/images/flags/" + team.emblemUrl} />, Text(team.localizedName))

  def standings = {
    val currentGroupName = S.param("group") openOr Group.all.head.name
    val currentGroup = Group.forName(currentGroupName)
    val tableHtml = SHtml.idMemoize( table => {
      ".standing" #> currentGroup.standings.zipWithIndex.map(x => {
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
    "#group_select" #> {
      "#group_name" #> (S.?("group") + " %s").format(currentGroup.name) &
      "#group_items" #> {
        "li *" #> Group.all.map(x => <a href={"/standings/%s".format(x.name)}>{S.?("group") + " %s".format(x.name)}</a>)
      }
    } &
    "#standings" #> tableHtml
  }

}
