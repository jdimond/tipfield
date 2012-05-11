package de.dimond.tippspiel.model

import net.liftweb.common._

import net.liftweb.db.{StandardDBVendor, DB, DefaultConnectionIdentifier, DBLogEntry}
import net.liftweb.mapper.{MetaMapper, Schemifier}
import net.liftweb.util.Props

import de.dimond.tippspiel.model.mapper._

object PersistanceConfiguration {
  private var _initialized = false
  private val tables: Seq[MetaMapper[_]] = Seq(DbResult, DbUser, DbTip, DbExtendedSession,
                                               DbPool, DbPoolMembership, DbFriends, DbPoolInvites,
                                               DbSpecialTip, DbFacebookRequests)

  private val logger = Logger(PersistanceConfiguration.getClass)

  def initialized = _initialized
  def initialize() = {
    if (_initialized) throw new IllegalStateException("PersistanceConfiguration already initliazed!")
    _initialized = true
    val database = Props get "db.url" openOr {
      if (Props.testMode) {
        "jdbc:postgresql://localhost/tippspieltest"
      } else {
        "jdbc:postgresql://localhost/tippspiel"
      }
    }
    val dbVendor = new StandardDBVendor(Props get "db.driver" openOr "org.postgresql.Driver",
                                        Props get "db.url" openOr database,
                                        Props get "db.username", Props get "db.password")
    DB.defineConnectionManager(DefaultConnectionIdentifier, dbVendor)

    if (Props.testMode) {
      Schemifier.destroyTables_!!(Schemifier.infoF _, tables: _*)
    }
    Schemifier.schemify(true, Schemifier.infoF _, tables: _*)
    if (!Props.productionMode) {
      DB.addLogFunc {
        case (query, time) => {
          logger.debug("All queries took " + time + "ms: ")
          query.allEntries.foreach({ case DBLogEntry(stmt, duration) =>
            logger.debug(stmt + " took " + duration + "ms")})
          logger.debug("End queries")
        }
      }
    }
  }
  def flush_!() = {
    if (!Props.testMode) throw new IllegalStateException("Not allowed to flush DB when not in test mode!!!")
    tables.map(_.bulkDelete_!!())
  }

  def Result: MetaResult = DbResult
  def User: MetaUser[_ <: User] = DbUser
  def Tip: MetaTip = DbTip
  def ExtendedSession: ExtendedSession = DbExtendedSession
  def Pool: MetaPool = DbPool
  def SpecialTip: MetaSpecialTip = DbSpecialTip
  def FacebookRequests: MetaFacebookRequests = DbFacebookRequests
}
