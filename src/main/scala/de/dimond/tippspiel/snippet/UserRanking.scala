package de.dimond.tippspiel
package snippet

import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http.SHtml
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import Helpers._
import net.liftweb.mapper.By

import de.dimond.tippspiel.model.PersistanceConfiguration._

class UserRanking {
  import model._
  def global = {
    val ranking = User.userRanking(10)
    val rankingWithUser = User.currentUser match {
      case Full(user) => {
        if (ranking.map(_._2.id).contains(user.id)) ranking
        else ranking.dropRight(1) :+ (user.ranking, user)
      }
      case _ => ranking
    }
    ".leaderboard_entry" #> { rankingWithUser.map { case (rank, user) => {
      ".leaderboard_rank *" #> rank &
      ".full_name *" #> user.fullName &
      "img [src]" #> user.profilePictureUrl &
      ".leaderboard_points *" #> user.points
    }}}
  }
}
