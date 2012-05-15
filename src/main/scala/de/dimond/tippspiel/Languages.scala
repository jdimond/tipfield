package de.dimond.tippspiel
import java.util.Locale
import net.liftweb.http.S

object Languages {

  val supportedLanguages = Seq(
    Lang(Locale.ENGLISH, "great_britain.png"),
    Lang(Locale.GERMAN, "germany.png"))
    //Lang(Locale.FRENCH, "france.png")) /* TODO: Complete translation */

    case class Lang(locale: Locale, flag: String)
}
