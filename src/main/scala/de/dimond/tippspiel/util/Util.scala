package de.dimond.tippspiel.util

object Util {
  def parseInt(s: String) = try { Some(s.toInt) } catch { case _ => None }
}
