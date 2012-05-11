package de.dimond.tippspiel.util

import java.util.Locale
import java.io.InputStream
import scala.io.Source

import net.liftweb.util.Helpers
import net.liftweb.common._

object Util {
  def parseInt(s: String) = try { Some(s.toInt) } catch { case _ => None }
  def isToString(is: InputStream) = Source.fromInputStream(is).getLines().mkString("\n")
  def queryParams(params: String): Map[String, List[String]] = {
    val kvPairs = for {
      nameVal <- params.split("&").toList.map(_.trim).filter(_.length > 0)
      (name, value) <- nameVal.split("=").toList match {
        case Nil => Empty
        case n :: v :: _ => Full((Helpers.urlDecode(n), Helpers.urlDecode(v)))
        case n :: _ => Full((Helpers.urlDecode(n), ""))
      }
    } yield (name, value)

    kvPairs.foldLeft(Map[String, List[String]]()) {
      case (map, (name, value)) => map + (name -> (map.getOrElse(name, Nil) ::: List(value)))
    }
  }

}

object Int {
  def unapply(s : String) : Option[Int] = try {
    Some(s.toInt)
  } catch {
    case _ : java.lang.NumberFormatException => None
  }
}
object Long {
  def unapply(s : String) : Option[Long] = try {
    Some(s.toLong)
  } catch {
    case _ : java.lang.NumberFormatException => None
  }
}
