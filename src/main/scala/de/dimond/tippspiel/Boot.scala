package de.dimond.tippspiel

import net.liftweb._
import net.liftweb.util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.lib._
import de.dimond.tippspiel.model.PersistanceConfiguration._
import java.util.Locale
import net.liftweb.http.provider.HTTPRequest
import de.dimond.tippspiel.util.Util

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Bootable with Logger {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("de.dimond.tippspiel")

    val ifLoggedIn = If (() => User.loggedIn_?, () => RedirectResponse("/login"))
    val ifAdmin = If(() => Props.devMode || User.superUser_?, () => RedirectResponse("/index"))

    // Build SiteMap
    val entries = List(
      //Menu.i("Home") / "index", // the simple way to declare a menu
      Menu(Loc("Home", List("index") -> false, S.?("home"), ifLoggedIn)),
      Menu(Loc("Schedule", List("schedule") -> false, S.?("schedule"), ifLoggedIn)),
      Menu(Loc("Standings", List("standings") -> false, S.?("standings"))),
      Menu(Loc("My Pools", List("pools") -> false, S.?("my_pools"), ifLoggedIn)),
      Menu(Loc("Admin", List("admin") -> false, S.?("admin"), ifAdmin)),
      Menu.i("login") / "login" >> Hidden
    )

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMap(SiteMap(entries:_*))

    LiftRules.dispatch.append {
      case req@Req(List("logout"), _, _) => () => { User.logout; S.redirectTo("/") }
      case req@Req(List("facebook", "authorize"), _, _) => () => FbLogin.authorize(req)
      case req@Req(List("facebook", "callback"), _, _) => () => FbLogin.callback(req)
    }

    // Don't serve static content
    LiftRules.liftRequest.append {
      case Req("classpath" :: _, _, _) => true
      case Req("ajax_request" :: _, _, _) => true
      case Req("favicon" :: Nil, "ico", GetRequest) => false
      case Req("static" :: _, _, _) => false
      case Req(_, "css", GetRequest) => false
      case Req(_, "js", GetRequest) => false
    }

    LiftRules.statelessRewrite.append {
      case RewriteRequest(ParsePath(List("schedule", matchday),_,_,_),_,_) =>
         RewriteResponse("schedule" :: Nil, Map("matchday" -> matchday))
      case RewriteRequest(ParsePath(List("standings", group),_,_,_),_,_) =>
         RewriteResponse("standings" :: Nil, Map("group" -> group))
      case RewriteRequest(ParsePath(List("pools", poolid),_,_,_),_,_) =>
         RewriteResponse("pools" :: Nil, Map("poolid" -> poolid))

    }

    // Make sure ExtendedSession is used
    LiftRules.earlyInStateful.append(ExtendedSession.testCookieEarlyInStateful)

    // Use jQuery 1.4
    LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQuery14Artifacts

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))

    // Use our own Locale calculation method and resources
    LiftRules.localeCalculator = localeCalculator _
    LiftRules.resourceNames = "i18n/messages" :: LiftRules.resourceNames

    GameData.init(Props.testMode || Props.devMode)

    PersistanceConfiguration.initialize()
  }

  /** Determine user locale to serve site in right language:
   * - Logged in: Fb locale
   * - Logged out: Browser locale
   */
  def localeCalculator(request : Box[HTTPRequest]): Locale = User.currentUser match {
    case Full(user) => user.locale.map(Util.localeFromString(_)).getOrElse(calculateLocaleFromRequest(request))
    case _ => calculateLocaleFromRequest(request)
  }

  private def calculateLocaleFromRequest(request: Box[HTTPRequest]): Locale = {
    request.flatMap(r => {
      def calcLocale: Box[Locale] = Full(LiftRules.defaultLocaleCalculator(request))
      S.param("locale") match {
        case Full(null) => calcLocale
        case f@Full(selectedLocale) => tryo(Util.localeFromString(selectedLocale))
        case _ => calcLocale
      }
    }).openOr(Locale.getDefault())
  }

}

