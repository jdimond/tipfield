package de.dimond.tippspiel.util

import org.scala_tools.time.Imports._

object DateHelpers {
  private val format = DateTimeFormat.forPattern("dd.MM.yy HH:mm");
  def formatTime(dateTime: DateTime) = format.print(dateTime)

  object Implicits {
    implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
  }
}
