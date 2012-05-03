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
import model._

object UserRanking {
  def rankingTable(ranking: Seq[(Rank, User)]) = {
    ".ranking_entry" #> { ranking.map { case (rank, user) => {
      ".ranking_rank *" #> rank.is &
      ".ranking_full_name *" #> user.fullName &
      "img [src]" #> user.profilePictureUrl &
      ".ranking_points *" #> user.points
    }}}
  }
}

class UserRanking {
  def global = {
    val ranking = User.userRanking(10)
    val rankingWithUser = User.currentUser match {
      case Full(user) => {
        if (ranking.map(_._2.id).contains(user.id)) ranking
        else ranking.dropRight(1) :+ (user.ranking, user)
      }
      case _ => ranking
    }
    "" #> ""
  }
}
