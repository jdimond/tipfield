package de.dimond.tippspiel
package snippet

import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.SHtml._
import net.liftweb.http.S._
import net.liftweb.http.js._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery._
import Helpers._
import net.liftweb.mapper.By

import org.scala_tools.time.Imports._

import de.dimond.tippspiel._
import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._
import de.dimond.tippspiel.util._

object PoolName extends RequestVar("")
object PoolDescription extends RequestVar("")
object AllowMemberInvite extends RequestVar(true)

class Pools {
  val user = User.currentUser open_!

  def linkForPool(pool: Pool) = pool match {
    case FacebookPool => "/pools"
    case p => "/pools/%d".format(p.id)
  }

  case object FacebookPool extends Pool {
    def id = 0
    def name = ?("facebook_friends")
    def description = ""
    def users = user.friends + user.id
    def adminId = 0
    def removeUser(user: User) = throw new RuntimeException("Not supported")
    def addUser(user: User) = throw new RuntimeException("Not supported")

    def userHasLeftGroup(userId: Long) = Full(false)
    def inviteUser(facebookId: String, fromUser: Option[User]) = throw new RuntimeException("Not supported")
    def userIsAllowedToInvite(user: User) = false
    def userIsInvited(facebookId: String) = false
    def ignoreInvitations(user: User) = throw new RuntimeException("Not supported")
  }

  def pools = Seq(FacebookPool) ++ Pool.allForUser(user).toSeq.sortBy(_.name)

  val currentPool = S.param("poolid") match {
    case Full(Long(id)) => {
      Pool.forId(id) match {
        case Full(p) => {
          if (p.users.contains(user.id)) p
          else S.redirectTo(linkForPool(FacebookPool))
        }
        case _ => S.redirectTo(linkForPool(FacebookPool))
      }
    }
    case Full(_) => S.redirectTo(linkForPool(FacebookPool))
    case _ => FacebookPool
  }

  def poolRanking = {
    val users = for {
      userId <- currentPool.users.toSeq
      user <- User.findById(userId)
    } yield user
    UserRanking.rankingTable(User.rankUsers(users))
  }

  def createPoolForm = {
    def process() = {
      if (PoolName.is.length == 0) {
        S.appendJs(JsCmds.Run("$('#pool_name_group').addClass('error')"))
      } else {
        Pool.newPool(PoolName.is, PoolDescription.is, AllowMemberInvite.is, user) match {
          case Full(pool) => {
            PoolName.remove()
            PoolDescription.remove()
            AllowMemberInvite.remove()
          }
          case _ => S.error("There was an unexpected error")
        }
      }
    }
    "#create_pool_form" #> {
      "name=pool_name" #> SHtml.text(PoolName.is, PoolName(_)) &
      "name=pool_description" #> SHtml.textarea(PoolDescription.is, PoolDescription(_)) &
      "name=allow_member_invite" #> SHtml.checkbox(AllowMemberInvite.is, AllowMemberInvite(_)) &
      "name=process" #> SHtml.hidden(process)
    }
  }

  def poolList = ".pool_entry" #> (pools map { pool =>
    ".pool_link [href]" #> linkForPool(pool) &
    ".pool_button *" #> pool.name
  })

  def ajaxFriendsButtonResponseHandler(response: Any): JsCmd = {
    Full(response).asA[Map[String, Any]] match {
      case Full(r) => {
        (r.get("request"), r.get("to")) match {
          case (Some(rid), Some(ids: Seq[_])) => {
            currentPool match {
              case FacebookPool => {
                debug("Invited to facebook pool: %s".format(currentPool))
              }
              case realPool => {
                val strIds = ids.collect { case str: String => str }
                strIds.map(realPool.inviteUser(_, Some(user)))
                debug("Added following invites: %s".format(strIds))
                _Noop /* TODO: inform user */
              }
            }
          }
          case other => {
            debug("Wrong type: %s".format(other))
            _Noop
          }
        }
      }
      case other => {
        debug("Not a JSON: %s".format(other))
        _Noop
      }
    }
  }

  def inviteFriendsButton = {
    if (currentPool.userIsAllowedToInvite(user)) {
      val ajaxCall = SHtml.jsonCall(JsRaw("response"), ajaxFriendsButtonResponseHandler)
      "#invite_friends_button [onclick]" #> {
        "FB.ui({method: 'apprequests', message: '%s'}, function(response) { %s });".format(
          "Invitation to Tippspiel", // TODO: make sure it is escaped correctly
          ajaxCall._2.toJsCmd
        )
        //"(function(response) { " + ajaxCall._2.toJsCmd + "})({ request: '123235346', to: ['1234', '2345'] });"
      }
    } else {
      "#invite_friends_section" #> ""
    }
  }

  def leavePoolButton = {
    currentPool match {
      case FacebookPool => {
        "#leave_pool_button" #> ""
      }
      case p: Pool => {
        def process() = {
          p.removeUser(user)
        }
        "name=process" #> SHtml.hidden(process)
      }
    }
  }

  def invitations = {
    val invitations = Pool.invitationsForUser(user)
    if(invitations.size > 0) {
      "#pool_invitations" #> {
        invitations map { pool =>
          val id = Helpers.nextFuncName
          ".invitation_entry [id]" #> id &
          ".pool_name *" #> pool.name &
          ".join_link *" #> { body => SHtml.a(() => {
            pool.addUser(user)
            S.redirectTo(linkForPool(pool))
          }, body) } &
          ".ignore_link *" #> { body => SHtml.a(() => {
            pool.ignoreInvitations(user)
            JqJsCmds.FadeOut(id, TimeSpan(0), TimeSpan(700))
          }, body) }
        }
      }
    } else {
      "#pool_invitations" #> ""
    }
  }

  def poolName = "* *" #> currentPool.name
}
