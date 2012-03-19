package de.dimond.tippspiel

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._
import db.{StandardDBVendor, DB, DefaultConnectionIdentifier}
import mapper.Schemifier

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.lib._


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Bootable {
  def setupDb() {
    import de.dimond.tippspiel.model._
    import java.util.Date

    def date(s: String) = new Date()

    Result.bulkDelete_!!()

    Result.create.gameId(1).goalsHome(1).goalsAway(0).save
    Result.create.gameId(2).goalsHome(0).goalsAway(2).save
    Result.create.gameId(3).goalsHome(2).goalsAway(3).save
    Result.create.gameId(4).goalsHome(4).goalsAway(1).save
    Result.create.gameId(5).goalsHome(0).goalsAway(0).save
    Result.create.gameId(6).goalsHome(2).goalsAway(1).save
    Result.create.gameId(7).goalsHome(0).goalsAway(0).save
    Result.create.gameId(8).goalsHome(3).goalsAway(0).save
    Result.create.gameId(9).goalsHome(1).goalsAway(1).save
    Result.create.gameId(10).goalsHome(1).goalsAway(3).save
    Result.create.gameId(11).goalsHome(1).goalsAway(2).save
    Result.create.gameId(12).goalsHome(1).goalsAway(2).save
    Result.create.gameId(13).goalsHome(3).goalsAway(3).save
    Result.create.gameId(14).goalsHome(0).goalsAway(0).save
    Result.create.gameId(15).goalsHome(3).goalsAway(1).save
    Result.create.gameId(16).goalsHome(1).goalsAway(1).save
    Result.create.gameId(17).goalsHome(0).goalsAway(0).save
    Result.create.gameId(18).goalsHome(2).goalsAway(3).save
    Result.create.gameId(19).goalsHome(1).goalsAway(4).save
    Result.create.gameId(20).goalsHome(4).goalsAway(2).save
  }
  def boot {
    // where to search snippet
    LiftRules.addToPackages("de.dimond.tippspiel")

    val ifLoggedIn = If (() => User.loggedIn_?, () => RedirectResponse("/login"))

    // Build SiteMap
    val entries = List(
      //Menu.i("Home") / "index", // the simple way to declare a menu
      Menu(Loc("Home", List("index") -> false, "Home", ifLoggedIn)),
      Menu(Loc("Spielplan", List("schedule") -> false, "Schedule", ifLoggedIn)),
      Menu(Loc("Standings", List("standings") -> false, "Standings")),
      Menu.i("Login") / "login" >> Hidden
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
      case Req(_, "css", GetRequest) => false
      case Req(_, "js", GetRequest) => false
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

    val dbVendor = new StandardDBVendor(Props get "db.driver" openOr "org.postgresql.Driver",
                                        Props get "db.url" openOr "jdbc:postgresql://localhost/tippspiel",
                                        Empty, Empty)
    DB.defineConnectionManager(DefaultConnectionIdentifier, dbVendor)
    Schemifier.schemify(true, Schemifier.infoF _, Result, Tip, User, ExtendedSession)

    setupDb()

    GameData.init()
  }
}
