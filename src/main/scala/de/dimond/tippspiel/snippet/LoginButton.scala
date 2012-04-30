package de.dimond.tippspiel
package snippet

//import scala.xml.{NodeSeq, Text}
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http.SHtml
import Helpers._

import de.dimond.tippspiel.model.PersistanceConfiguration._

class LoginButton {
  def render = User.currentUser match {
    case Full(user) => {
      "#login_link [href]" #> "/logout" &
      "img [src]" #> user.profilePictureUrl &
      "#user_name *" #> user.fullName &
      "#login_link *" #> "Logout"
    }
    case _ => {
      "#login_link [href]" #> "/facebook/authorize" &
      "img [src]" #> "/images/fb_logo.png" &
      "#user_name *" #> "Login"
    }
  }
}
