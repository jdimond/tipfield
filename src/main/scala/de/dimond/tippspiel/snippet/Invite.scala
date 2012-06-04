package de.dimond.tippspiel
package snippet

import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.actor._
import net.liftweb.http._
import net.liftweb.http.SHtml._
import net.liftweb.http.S._
import net.liftweb.http.js._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.http.js.jquery.JqJE._
import Helpers._
import net.liftweb.mapper.By
import scala.actors.Actor
import org.scala_tools.time.Imports._
import de.dimond.tippspiel._
import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._
import de.dimond.tippspiel.util._
import de.dimond.tippspiel.lib._
import scala.xml.Text

class Invite extends Logger {
  val userBox: Box[User] = User.currentUser

  def render = {
    val invitationBox = S.param("inviteid").flatMap(Pool.findInvitationById(_))
    val poolBox = invitationBox.flatMap(i => Pool.forId(i.poolId))
    val invitingUserBox = invitationBox.flatMap(i => User.findById(i.userId))
    (poolBox, invitingUserBox) match {
      case (Full(pool), Full(invitingUser)) =>
        "#pool_name" #> "\"%s\"".format(pool.name) &
        "#inviting_name" #> invitingUser.fullName &
        "#howto" #> (userBox match {
          case Full(user) => "*" #> ""
          case _ => "*" #> { body => body }
        }) &
        "#pool_section" #> (userBox match {
          case Full(user) => {
            "#join_pool_link" #> (body => {
              def join = {
                pool.addUser(user)
                S.redirectTo("/pools/%d".format(pool.id))
              }
              val newBody = body match {
                case e: scala.xml.Elem => e.child
                case n => n
              }
              SHtml.a(join _, newBody, "class" -> "btn btn-large btn-success")
            })
          }
          case _ => "*" #> ""
        }) &
        "#join_section" #> (userBox match {
          case Full(user) => "*" #> ""
          case _ => "#fblink [href]" #> {
            val redirect = urlEncode(S.request.map(_.uri).openOr(""))
            "/facebook/authorize?redirect=%s".format(redirect)
          }
        })
      case (_, _) => {
        warn("Error locating invitation!\nParam: %s\nInvitation: %s\nPool: %s\nUser: %s".format(
              S.param("inviteid"), invitationBox, poolBox, invitingUserBox))
        "*" #> S.?("oops_could_not_find_link")
      }
    }
  }
}
