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

    Game.bulkDelete_!!()
    Result.bulkDelete_!!()
    Team.bulkDelete_!!()
    Tip.bulkDelete_!!()

    val bayern = Team.create.name("FC Bayern München")
    bayern.save
    val dortmund = Team.create.name("BVB Dortmund 09")
    dortmund.save
    val bremen = Team.create.name("Werder Bremen")
    bremen.save
    val schalke = Team.create.name("FC Schalke 04")
    schalke.save

    val one = Game.create.teamHome(bayern).teamAway(dortmund).date(date("2012-06-13T13:30GMT+2:00"))
    one.save()
    Result.create.game(one).goalsHome(3).goalsAway(1).save()
    Game.create.teamHome(bremen).teamAway(schalke).date(date("2012-06-13T13:30GMT+2:00")).save()
    Game.create.teamHome(bayern).teamAway(schalke).date(date("2012-06-13T13:30GMT+2:00")).save()
    Game.create.teamHome(dortmund).teamAway(bremen).date(date("2012-06-13T13:30GMT+2:00")).save()
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
      Menu(Loc("Ranking", List("ranking") -> false, "Ranking", ifLoggedIn)),
      Menu.i("Login") / "login" >> Hidden
    )

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMap(SiteMap(entries:_*))

    LiftRules.dispatch.append {
      case req@Req(List("logout"), _, _) => () => { User.logout; Full(RedirectResponse("/")) }
      case req@Req(List("facebook", "authorize"), _, _) => () => FbLogin.authorize(req)
      case req@Req(List("facebook", "callback"), _, _) => () => FbLogin.callback(req)
    }

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
    Schemifier.schemify(true, Schemifier.infoF _, Game, Result, Team, Tip, User)

    setupDb()
  }
}
