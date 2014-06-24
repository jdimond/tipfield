package de.dimond.tippspiel
package snippet

import org.scala_tools.time.Imports._
import de.dimond.tippspiel.model.PersistanceConfiguration._
import de.dimond.tippspiel.model.{ Special, SpecialTip }
import de.dimond.tippspiel.util._
import net.liftweb.common._
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.S
import net.liftweb.util.Helpers._
import net.liftweb.util._
import de.dimond.tippspiel.model.FacebookPool

class SpecialOverview {

  val special = S.param("specialId") match {
    case Full(Long(specialId)) => Special.forId(specialId) match {
      case Some(s) => s
      case None => S.redirectTo("/")
    }
    case _ => S.redirectTo("/")
  }

  val users = (S.param("showAdmin"), User.currentUser) match {
    case (Full("true"), Full(user)) if user.isAdmin => User.findAll()
    case (_, Full(user)) => User.findAll(user.poolFriends + user.id)
    case _ => Seq()
  }

  def specialRanking = {
    val ranking = User.rankUsers(users)
    ".ranking_entry" #> {
      ranking.map {
        case (rank, user) => {
          ".ranking_rank *" #> rank.is &
          ".ranking_full_name *" #> <a href={ "/tips/%s".format(user.fbId) }>{ user.fullName }</a> &
          ".ranking_user_image [src]" #> user.profilePictureUrl &
          ".special_answer" #> SpecialSnippet.selectHtml(user, special, SpecialTip.answersForUser(user, Seq(special))) &
          ".ranking_points *" #> user.points
        }
      }
    }
  }

  def specialHeader = {
    "#special_title" #> special.localizedTitle &
    "#special_points *" #> (special.points + " " + S.?("points"))
  }
}
