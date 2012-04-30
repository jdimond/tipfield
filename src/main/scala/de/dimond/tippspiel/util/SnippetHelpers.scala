package de.dimond.tippspiel.util

import de.dimond.tippspiel._

import model._

import scala.xml._

object SnippetHelpers {
  def teamHtml(ref: TeamReference) = ref.team match {
    case Left(str) => Text(str)
    case Right(team) => {
      Seq(<img src={"/images/flags/" + team.emblemUrl} />, Text(team.name))
    }
  }
}
