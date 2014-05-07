package de.dimond.tippspiel
package snippet

//import scala.xml.{NodeSeq, Text}
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http.{SHtml, S}

import Helpers._

import de.dimond.tippspiel.model.User
import de.dimond.tippspiel.model.PersistanceConfiguration._
import de.dimond.tippspiel.util.Util
import de.dimond.tippspiel.Languages

class LoginButton {
  def render = (User.currentUser match {
    case Full(user) => {
      "#login_link [href]" #> "/logout" &
      "#user_img [src]" #> user.profilePictureUrl &
      "#user_name *" #> user.fullName &
      "#login_link *" #> S.?("logout") &
      "#login_button" #> ""
    }
    case _ => {
      "#userbox *" #> "" &
      "#userbox" #> ""
      /*
      "#login_link [href]" #> "/facebook/authorize" &
      "#user_img [src]" #> "/images/fb_logo.png" &
      "#user_name *" #> S.?("login")
      */
    }
  })
}

class LanguageSelect {
  def render = {
    "#current_lang_flag [src]" #> ("/images/flags/" + S.?("flag_image")) &
    ".lang_element" #> (Languages.supportedLanguages
        filter(S.locale.getLanguage() != _.locale.getLanguage())
        map { lang =>
          ".lang_code [value]" #> lang.locale.toString &
          ".lang_flag [src]" #> ("/images/flags/" + lang.flag)
    })
  }
}
