package de.dimond.tippspiel.model

import net.liftweb.common._

import net.liftweb.db.{StandardDBVendor, DB, DefaultConnectionIdentifier, DBLogEntry}
import net.liftweb.mapper.Schemifier
import net.liftweb.util.Props

//import de.dimond.tippspiel.model.redis._
import de.dimond.tippspiel.model.mapper._

object PersistanceConfiguration extends Logger {
  private var initialized = false
  def initialize() = {
    if (initialized) throw new IllegalStateException("PersistanceConfiguration already initliazed!")
    initialized = true
    val dbVendor = new StandardDBVendor(Props get "db.driver" openOr "org.postgresql.Driver",
                                        Props get "db.url" openOr "jdbc:postgresql://localhost/tippspiel",
                                        Empty, Empty)
    DB.defineConnectionManager(DefaultConnectionIdentifier, dbVendor)
    Schemifier.schemify(true, Schemifier.infoF _, DbResult, DbUser, DbTip, DbExtendedSession)
    DB.addLogFunc {
      case (query, time) => {
         info("All queries took " + time + "ms: ")
         query.allEntries.foreach({ case DBLogEntry(stmt, duration) =>
            info(stmt + " took " + duration + "ms")})
            info("End queries")
      }
    }
  }
  def Result: MetaResult = DbResult
  def User: MetaUser[_ <: User] = DbUser
  def Tip: MetaTip = DbTip
  def ExtendedSession: ExtendedSession = DbExtendedSession
}
