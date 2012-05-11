package de.dimond.tippspiel
package snippet

//import scala.xml.{NodeSeq, Text}
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.json.JsonAST._
import net.liftweb.http.js.JsCmds._
import Helpers._

import org.scala_tools.time.Imports._

import de.dimond.tippspiel._
import de.dimond.tippspiel.lib._
import de.dimond.tippspiel.util._
import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._

class Canvas extends Logger {
  def error(errorStr: String) = "*" #> errorStr

  def canvasHandler = {
    S.param("signed_request") match {
      case Full(signedRequest) => {
        FacebookApi.api.parseSignedRequest(signedRequest) match {
          case Some(json) => {
            (json \ "user_id", json \ "oauth_token") match {
              case (JString(uid), JString(accessToken)) => {
                val expiresAt = (json \ "expires") match {
                  case JInt(at) => Some(new DateTime(at.longValue*1000))
                  case _ => None
                }
                FacebookApi.api.updateUserInfo(accessToken, expiresAt)
                S.redirectTo("/pools")
              }
              case _ => {
                S.param("error") match {
                  case Full(error) => {
                    /* TODO: Do something nicer than redirect to front page*/
                    S.redirectTo("/")
                  }
                  case _ => {
                    val url = "https://www.facebook.com/dialog/oauth/?client_id=%s&redirect_uri=%s".format(
                      urlEncode(FacebookApi.appId), urlEncode(FacebookApi.appUrl))
                    "*" #> {
                      Script(Run("window.top.location = %s".format(encJs(url)))) ++
                      <a target="_top" href={ url }>{S.?("redirect")}</a>
                    }
                  }
                }
              }
            }
          }
          case None => {
            warn("Error parsing signed_request: %s".format(signedRequest))
            error("An unexpected error occured!")
          }
        }
      }
      case _ => error("Page was not loaded inside a Facebook canvas!")
    }
  }
}
