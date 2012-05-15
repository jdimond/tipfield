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

object PoolName extends RequestVar("")
object PoolDescription extends RequestVar("")
object AllowMemberInvite extends RequestVar(true)

class Pools extends Logger {
  val user = User.currentUser open_!

  def linkForPool(pool: Pool) = pool match {
    case FacebookPool => "/pools"
    case p => "/pools/%d".format(p.id)
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

  lazy val poolUsers = User.findAll(currentPool.users)

  def poolRanking = {
    UserRanking.rankingTable(User.rankUsers(poolUsers))
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
            S.redirectTo(linkForPool(pool))
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
    ".pool_button *" #> (if (currentPool.id == pool.id) <b>{pool.name}</b> else <span>{pool.name}</span>) &
    ".pool_button [class+]" #> (if (currentPool.id == pool.id) "btn-inverse" else "")
  })

  def ajaxFriendsButtonResponseHandler(response: Any): JsCmd = {
    val success = alertSuccess(?("alert_success_title"), ?("alert_invited_friends"))
    val failure = alertFailure(?("alert_failure_title"), ?("unexpected_error_occured"))
    Full(response).asA[Map[String, Any]] match {
      case Full(r) => {
        (r.get("request"), r.get("to")) match {
          case (Some(rid: String), Some(ids: Seq[_])) => {
            val strIds = ids.collect { case str: String => str }
            for (strId <- strIds) {
              FacebookRequests.saveRequestForUser(user, strId, rid, currentPool.id)
            }
            currentPool match {
              case FacebookPool => {
                debug("Invited to facebook pool: %s".format(currentPool))
                success
              }
              case realPool => {
                strIds.map(realPool.inviteUser(_, Some(user)))
                debug("Added following invites: %s".format(strIds))
                success
              }
            }
          }
          case other => {
            warn("Wrong type: %s".format(other))
            failure
          }
        }
      }
      case other => {
        /* cancelled */
        Noop
      }
    }
  }

  def alertSuccess(title: String, message: String) = {
    JqId("alert") ~> JqAttr("class", "alert alert-success") &
    alert(title, message)
  }

  def alertFailure(title: String, message: String) = {
    JqId("alert") ~> JqAttr("class", "alert alert-error") &
    alert(title, message)
  }

  def alert(title: String, message: String) = {
    JqId("alert_title") ~> JqText(title) &
    JqId("alert_text") ~> JqText(message) &
    Show("alert")
  }

  def inviteFriendsButton = {
    if (currentPool.userIsAllowedToInvite(user)) {
      val ajaxCall = SHtml.jsonCall(JsRaw("response"), ajaxFriendsButtonResponseHandler)
      val excludeIds = "[%s]".format(poolUsers.map(u => encJs(u.fbId)).mkString(", "))
      "#invite_friends_button [onclick]" #> {
        "FB.ui({method: 'apprequests', message: %s, exclude_ids: %s}, function(response) { %s });".format(
          encJs(?("invitation_request")),
          excludeIds,
          ajaxCall._2.toJsCmd
        )
      }
    } else {
      "#invite_friends_section" #> ""
    }
  }

  def leavePoolButton = {
    currentPool match {
      case FacebookPool => {
        "#leave_pool" #> ""
      }
      case p: Pool => {
        def process() = {
          p.removeUser(user)
        }
        "name=process" #> SHtml.hidden(process) &
        "#leave_pool_button [onclick]" #> {
          "if (confirm('Really leave pool')) { $('#leave_pool_form').submit(); }" // TODO: language
        }
      }
    }
  }

  def checkInvitationRequests(invitations: Set[Pool]) = {
    val openPools = invitations.map(_.id)
    val toDelete = FacebookRequests.getRequests(user.fbId).filter(req => !openPools.contains(req.poolId))
    if (toDelete.size > 0) FacebookRequestDeleter ! (user, toDelete)
  }

  def invitations = {
    val invitations = Pool.invitationsForUser(user)
    checkInvitationRequests(invitations)
    if(invitations.size > 0) {
      "#pool_invitations" #> {
        invitations map { pool =>
          val id = Helpers.nextFuncName
          ".invitation_entry [id]" #> id &
          ".pool_name *" #> pool.name &
          ".join_link *" #> { body => SHtml.a(() => {
            FacebookRequestDeleter ! (user, FacebookRequests.getRequests(user.fbId, pool.id))
            pool.addUser(user)
            S.redirectTo(linkForPool(pool))
          }, body) } &
          ".ignore_link *" #> { body => SHtml.a(() => {
            FacebookRequestDeleter ! (user, FacebookRequests.getRequests(user.fbId, pool.id))
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
  def poolDescription = "* *" #> currentPool.description
}
