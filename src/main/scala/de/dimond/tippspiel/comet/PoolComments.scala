package de.dimond.tippspiel
package comet

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

case class CommentList(list: List[PoolComment])

case class JqAfter(content: scala.xml.NodeSeq) extends JsExp with JsMember {
  override val toJsCmd = fixHtmlFunc("inline", content){"after(" + _ + ")"}
}

case class JqBefore(content: scala.xml.NodeSeq) extends JsExp with JsMember {
  override val toJsCmd = fixHtmlFunc("inline", content){"before(" + _ + ")"}
}

object PoolComments {
  val template = <tr><td>
      <img class="comment_img" height="50px" width="50px" src="https://graph.facebook.com/1/picture"/>
      <span class="comment_time">20:15</span>
      <div><strong class="comment_name">Name</strong></div>
      <span class="comment_text">Comment text</span>
    </td></tr>
}

class PoolComments extends CometActor with CometListener with Logger {
  val pool: Box[Pool] = S.param("poolid") collect { case Long(id) => id } flatMap { Pool.forId(_) }
  val server: LiftActor = pool flatMap { p => CommentServerManager.getServer(p.id) } openOr DummyServer

  override def registerWith = server

  var allComments: List[PoolComment] = server match {
    case cs: CommentServer => {
      cs.comments
    }
    case _ => List()
  }

  var currentComments: List[PoolComment] = allComments.take(5)

  def commentCssSel(comment: PoolComment, users: Map[Long, User]) = {
    users.get(comment.userId) match {
      case Some(user) => {
        ".comment_img [src]" #> user.profilePictureUrl &
        ".comment_time *" #> DateHelpers.formatTime(comment.commentDate) &
        ".comment_name *" #> user.fullName &
        ".comment_text" #> SnippetHelpers.replaceNewlinesWithBrs(comment.comment)
      }
      case None => {
        warn("User with ID %d not found!".format(comment.userId))
        "td *" #> S.?("unexpected_error_occured")
      }
    }
  }

  def commentListHtml(comments: List[PoolComment]) = {
    val users = User.findAll(comments.map(_.userId).toSet)
    val userMap = users.map(u => (u.id, u)).toMap
    comments.map(commentCssSel(_, userMap)(PoolComments.template)).reduceOption(_ ++ _) getOrElse Text("")
  }

  val onSuccess = JqId("comment_submit") ~> JsFunc("removeClass", "disabled") &
                  JqId("comment_textarea") ~> JsFunc("val", "")

  override def lowPriority = synchronized {
    case CommentList(list) => {
      allComments = list
      val newComments = currentComments.headOption match {
        case Some(first) => list.takeWhile(_.commentId != first.commentId)
        case _ => list.take(5)
      }
      val newCommentsJs = JqId("add_comment_row") ~> JqAfter(commentListHtml(newComments))
      val enableJs = if (currentlyDisabled) {
        currentlyDisabled = false
        onSuccess
      } else {
        Noop
      }
      currentComments = newComments ++ currentComments
      partialUpdate(enableJs & newCommentsJs)
    }
  }

  var currentlyDisabled = false

  override def render = {
    val onSubmitJs = JqId("comment_submit") ~> JsFunc("addClass", "disabled") &
                     JqId("comment_control_group") ~> JsFunc("removeClass", "error")
    val onErrorJs = JqId("comment_submit") ~> JsFunc("removeClass", "disabled") &
                    JqId("comment_control_group") ~> JsFunc("addClass", "error")

    var commentText = ""

    def process = {
      currentlyDisabled = true
      if (commentText.length == 0) {
        onErrorJs
      } else if (commentText.length > 2048) {
        onErrorJs
      } else {
        (pool, User.currentUser) match {
          case (Full(pool), Full(user)) => {
            PoolComment.saveComment(pool, user, commentText) match {
              case Full(comment) => {
                server ! comment
              }
              case _ =>
            }
          }
          case _ =>
        }
      }
    }

    def showMore: JsCmd = synchronized {
      val newComments = allComments.drop(currentComments.size).take(5)
      val newCommentsJs = JqId("show_more_comments_row") ~> JqBefore(commentListHtml(newComments))
      currentComments = currentComments ++ newComments
      val moreToShow = allComments.size - currentComments.size
      if (moreToShow <= 0) {
        newCommentsJs & JqId("show_more_comments_row") ~> JqReplace(Text(""))
      } else {
        newCommentsJs & JqId("show_more_comments_button") ~> JqText(S.?("show_more_button").format(moreToShow))
      }
    }

    "#add_comment_row" #> {
      User.currentUser match {
        case Full(user) => {
          "#comment_textarea" #> {
            SHtml.textarea("", commentText = _,
                           "id" -> "comment_textarea",
                           "placeholder" -> S.?("add_comment_placeholder")) ++
            SHtml.hidden(process _)
          } &
          "#comment_submit [value]" #> S.?("add_comment") &
          "#add_comment_img [src]" #> user.profilePictureUrl andThen
          "#add_comment_form" #> { body => SHtml.ajaxForm(body, onSubmitJs) }
        }
        case _ => "*" #> ""
      }
    } &
    "#comments_section" #> {
      commentListHtml(currentComments)
    } &
    "#show_more_comments_row" #> {
      val moreToShow = allComments.size - currentComments.size
      if (moreToShow <= 0) {
        "*" #> ""
      } else {
        "#show_more_comments_button" #> SHtml.a(showMore _, Text(S.?("show_more_button").format(moreToShow)),
                                                "class" -> "btn")
      }
    }
  }
}

object CommentServerManager extends Logger {
  private var _commentServers: Map[Long, CommentServer] = Map()

  def getServer(poolId: Long): Option[CommentServer] = synchronized {
    try {
      val server = _commentServers.get(poolId) getOrElse {
        val newServer = new CommentServer(poolId)
        _commentServers += (poolId -> newServer)
        newServer
      }
      debug("Found Comment server %s for pool id %d".format(server, poolId))
      Some(server)
    } catch {
      case e: IllegalArgumentException => {
        warn("Could not find Comment server!", e)
        None
      }
    }
  }
}

class CommentServer(poolId: Long) extends LiftActor with ListenerManager with Logger {
  import util.DateHelpers.Implicits._

  val pool = Pool.forId(poolId) match {
    case Full(pool) => pool
    case _ => throw new IllegalArgumentException("Pool not found")
  }

  private var _comments: List[PoolComment] = {
    val pc = PoolComment.commentsForPool(pool).toList
    pc.sortBy(_.commentDate).reverse.toList
  }

  def comments = _comments

  override def createUpdate = CommentList(comments)

  override def lowPriority = {
    case comment: PoolComment => {
      if (comment.poolId == this.poolId) {
        _comments = comment :: _comments
        updateListeners()
      } else {
        warn("Received comment with pool id %d for wrong pool %d".format(comment.poolId, this.poolId))
      }
    }
  }
}

object DummyServer extends LiftActor {
  override def messageHandler = {
    case _ => ()
  }
}
