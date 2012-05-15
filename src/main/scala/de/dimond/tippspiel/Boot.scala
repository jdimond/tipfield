package de.dimond.tippspiel

import net.liftweb._
import net.liftweb.http.provider.{HTTPRequest,HTTPCookie}
import net.liftweb.util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.lib._
import de.dimond.tippspiel.model.PersistanceConfiguration._
import de.dimond.tippspiel.util.Util

import java.util.Locale

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Bootable with Logger {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("de.dimond.tippspiel")

    val ifLoggedIn = If (() => User.loggedIn_?, () => RedirectResponse("/"))
    val ifAdmin = If(() => Props.devMode || User.superUser_?, () => RedirectResponse("/index"))

    // Build SiteMap
    val entries = List(
      //Menu.i("Home") / "index", // the simple way to declare a menu
      //Menu(Loc("Home", List("index") -> false, S.?("home"), ifLoggedIn)),
      Menu(Loc("My Tips", List("tips") -> false, S.?("my_tips"), ifLoggedIn)),
      Menu(Loc("My Pools", List("pools") -> false, S.?("my_pools"), ifLoggedIn)),
      Menu(Loc("Schedule", List("schedule") -> false, S.?("schedule"))),
      Menu(Loc("Standings", List("standings") -> false, S.?("standings"))),
      Menu(Loc("How It Works", List("howto") -> false, S.?("howto"))),
      Menu(Loc("Admin", List("admin") -> false, S.?("admin"), ifAdmin)),
      Menu.i("index") / "index" >> Hidden,
      Menu.i("login") / "login" >> Hidden,
      Menu.i("aboutus") / "aboutus" >> Hidden,
      Menu.i("privacypolicy") / "privacypolicy" >> Hidden,
      Menu.i("canvas") / "canvas" / "index" >> Hidden
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

    LiftRules.statefulRewrite.append {
      case RewriteRequest(ParsePath(List("index"),_,_,_),_,_) => {
        if (User.loggedIn_?) {
          RewriteResponse("tips" :: Nil)
        } else {
          RewriteResponse("login" :: Nil)
        }
      }
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

    GameData.init(Props.testMode || Props.devMode)
    SpecialData.init(Props.testMode || Props.devMode)

    PersistanceConfiguration.initialize()
  }

  // Determine user locale to serve site in right language
  // Properly convert a language tag to a Locale
  def computeLocale(tag : String) = tag.split(Array('-', '_')) match {
    case Array(lang) => new Locale(lang)
    case Array(lang, country) => new Locale(lang, country)
    case Array(lang, country, variant) => new Locale(lang, country, variant)
  }

  val localeCookieName = "locale"

  // Use our own Locale calculation method using cookie or browser locale
  LiftRules.localeCalculator = {
    case fullReq @ Full(req) => {
      // Check against a set cookie, or the locale sent in the request
      def currentLocale : Locale =
        S.findCookie(localeCookieName).flatMap {
          cookie => cookie.value.map(computeLocale)
        } openOr LiftRules.defaultLocaleCalculator(fullReq)

      // Check to see if the user explicitly requests a new locale
      S.param("locale") match {
        case Full(requestedLocale) if requestedLocale != null => {
          val computedLocale = computeLocale(requestedLocale)
          S.addCookie(HTTPCookie(localeCookieName, Full(requestedLocale), Full(S.hostName),
              Full("/"), Full(3600*24*365), Empty, Empty))
          computedLocale
        }
        case _ => currentLocale
      }
    }
    case _ => Locale.getDefault
  }

  // Use own internationalization resources
  LiftRules.resourceNames = "i18n/messages" :: LiftRules.resourceNames
}

