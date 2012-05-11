package de.dimond.tippspiel
package lib

import scala.actors.Actor

import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.json._

import java.io.InputStream

import org.joda.time._

import de.dimond.tippspiel.model.PersistanceConfiguration._
import de.dimond.tippspiel.model._
import de.dimond.tippspiel.util._
import de.dimond.tippspiel.util.Util._

object FacebookRequestDeleter extends Actor with Logger {
  def act = loop {
    react {
      case (user: User, idsAny: Seq[_]) => {
        val ids = idsAny collect { case req: FacebookRequest => req.requestId }
        user.fbAccessToken match {
          case Some(token) => {
            debug("Deleting FB requests for user: %s".format(user.id))
            for (rid <- ids) {
              val url = "https://graph.facebook.com/%s_%s?access_token=%s".format(rid, user.fbId, token)
              debug("Deleting Request: %s".format(url))
              val (res, str) = NetHelpers.httpDelete(url, isToString _)
              if (str == "true") {
                FacebookRequests.deleteRequest(user.fbId, rid)
              } else if (res == 404) {
                warn("Facebook request delete returned 404")
                FacebookRequests.deleteRequest(user.fbId, rid)
              } else {
                warn("Facebook Request not deleted! Status was %d. Url was %s. Response was: %s".format(res, url, str))
              }
            }
          }
          case None => warn("fbAccessToken was Empty!")
        }
      }
      case _ =>
    }
  }
  this.start
}

case class FbUser(id: String,
                  name: String,
                  username: Option[String],
                  first_name: Option[String],
                  middle_name: Option[String],
                  last_name: Option[String],
                  gender: Option[String],
                  locale: Option[String],
                  timezone: Option[String])

class FacebookApi(val secret: String) extends Logger {
  import javax.crypto.Mac
  import javax.crypto.spec.SecretKeySpec

  def parseSignedRequest(signedRequest: String): Option[JValue] = {
    val parts = signedRequest.split('.')
    if (parts.size != 2) {
      warn("Signed request did not have 2 parts: %d. Request was: %s".format(parts.size, signedRequest))
      return None
    }
    val signature = parts(0)
    val payload = parts(1)
    val contentOpt = JsonParser.parseOpt(base64UrlDecode(payload))

    contentOpt match {
      case Some(content) => {
        (content \ "algorithm") match {
          case JString(algorithm) => { /* TODO match insenstive */
            algorithm.toUpperCase match {
              case "HMAC-SHA256" => {
                val key = new SecretKeySpec(secret.getBytes("ASCII"), "hmacSHA256")
                val hmacSha256 = Mac.getInstance("hmacSHA256");
                hmacSha256.init(key)
                val expected = base64UrlEncode(hmacSha256.doFinal(payload.getBytes))
                if (expected != signature) {
                  warn("Signatures don't match! Signature: %s".format(new String(expected)))
                  None
                } else {
                  Some(content)
                }
              }
              case x => {
                warn("Unkown algorithm: %s".format(x))
                None
              }
            }
          }
          case x => {
            warn("Invalid JSON format for algorithm: %s".format(x))
            None
          }
        }
      }
      case None => None
    }
  }

  def updateUserInfo(accessToken: String, expires: Option[DateTime]) = {
    val meUrl = "https://graph.facebook.com/me?access_token=%s".format(accessToken)

    implicit val formats = DefaultFormats
    val json = NetHelpers.httpGet(meUrl, x => JsonParser.parse(isToString(x)))
    val fbUser = json.extract[FbUser]

    val user = User.findByFbId(fbUser.id) match {
      case Full(user) => {
        user.fullName = fbUser.name
        user.fbId = fbUser.id
        user
      }
      case Empty => User.create(fbUser.name, fbUser.id)
      case Failure(m, eb, _) => eb match {
        case Full(e) => throw new RuntimeException("Error occured during Facebook login: " + m, e)
        case _ => throw new RuntimeException("Error occured during Facebook login: " + m)
      }
    }

    user.setFbAccessToken(Some(accessToken), expires)

    user.fbUserName = fbUser.username
    user.firstName = fbUser.first_name
    user.middleName = fbUser.middle_name
    user.lastName = fbUser.last_name
    user.gender = fbUser.gender
    user.locale match {
      case Some(_) =>
      case None => user.locale = fbUser.locale
    }
    user.fbTimeZone = fbUser.timezone

    def getFriends(url: String): Seq[String] = {
      debug("Retrieving friends from URL: %s".format(url))
      val friendsJson = NetHelpers.httpGet(url, x => JsonParser.parse(isToString(x)))
      val data = (friendsJson \ "data")
      val ids = data match {
        case JArray(arr) => arr.map(_ \ "id").collect { case JString(s) => s }
        case other => {
          warn("Found wrong type for \"data\" Element: %s".format(other))
          Seq()
        }
      }
      val nextIds = (friendsJson \ "paging" \ "next") match {
        case JString(url) => getFriends(url)
        case JNothing => Seq()
        case jvalue => {
          warn("Friendlist contained unknown \"next\"-Element: %s".format(jvalue))
          Seq()
        }
      }
      ids ++ nextIds
    }

    val friendsUrl = "https://graph.facebook.com/me/friends?access_token=%s".format(accessToken)
    user.facebookFriends = getFriends(friendsUrl).toSet

    if (user.save) {
      User.logUserIn(user)
      FacebookRequestDeleter ! (user, FacebookRequests.getRequests(user.fbId, FacebookPool.id))
      true
    } else {
      false
    }
  }

  private def base64UrlDecode(str: String) = {
    new String(Helpers.base64Decode(str.replace('-', '+').replace('_', '/')), "UTF-8")
  }
  private def base64UrlEncode(bytes: Array[Byte]) = {
    Helpers.base64Encode(bytes).replace('+', '-').replace('/', '_').replace('=', ' ').trim
  }
}

object FacebookApi {
  val appId = Props get("fb.AppId") open_!
  val appSecret = Props get("fb.AppSecret") open_!
  val redirectUri = Props get("fb.CallbackUrl") open_!
  val appUrl = Props get("fb.AppUrl") open_!

  val api = new FacebookApi(appSecret)
}
