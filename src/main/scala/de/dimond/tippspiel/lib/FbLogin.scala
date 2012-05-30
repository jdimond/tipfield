package de.dimond.tippspiel
package lib

import scala.actors.Actor

import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.json._

import java.util.UUID
import java.io.InputStream

import org.joda.time._

import de.dimond.tippspiel.model.PersistanceConfiguration._
import de.dimond.tippspiel.model._
import de.dimond.tippspiel.util._
import de.dimond.tippspiel.util.Util._

object FbLogin {
  import FacebookApi._

  object csrfSessionCode extends SessionVar[Box[String]](Empty)

  def fullRedirectUri = "%s%s".format(S.hostAndPath, redirectUri)

  def authorize(req: Req, redirectUri: Option[String] = None): Box[LiftResponse] = {
    val code = UUID.randomUUID().toString()
    csrfSessionCode.set(Full(code))
    val uri = Helpers.urlEncode(redirectUri getOrElse fullRedirectUri)
    val dialogUrl = ("https://www.facebook.com/dialog/oauth?client_id=%s&redirect_uri=%s" +
                    "&state=%s").format(appId, uri, code)
    Full(RedirectResponse(dialogUrl, req))
  }

  def callback(req: Req): Box[LiftResponse] = {
    val sessionCode = csrfSessionCode.is.openOr("")
    val requestCode = req.param("state").openOr("")
    /* Disable CSRF for the moment */
    if (true || (sessionCode.length > 0 && sessionCode.equals(requestCode))) {
      val accessCode = req.param("code").openOr("")
      val tokenUrl = ("https://graph.facebook.com/oauth/access_token?client_id=%s&redirect_uri=%s" +
                      "&client_secret=%s&code=%s").format(appId, fullRedirectUri, appSecret, accessCode)
      val values = NetHelpers.httpGet(tokenUrl, x => queryParams(isToString(x)))
      val accessToken = (values.getOrElse("access_token", Nil) ::: List("")).head
      val expires = (values.getOrElse("expires", Nil) ::: List("0")).head

      api.updateUserInfo(accessToken, Some(new DateTime().plusSeconds(expires.toInt)))

      S.redirectTo("/")
    } else {
      SnippetHelpers.die("State doesn't match. You are probably a victim of CSRF")
    }
  }

}
