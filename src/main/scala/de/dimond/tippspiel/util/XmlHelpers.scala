package de.dimond.tippspiel.util

import scala.xml.transform._
import scala.xml._

object XmlHelpers {
  def uniquifyIds(ids: String => String)(nodeSeq: NodeSeq): NodeSeq = {
    val rr = new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case elem: Elem => {
          elem.attribute("id") match {
            case Some(Seq(Text(x))) => elem % new UnprefixedAttribute("id", Text(ids(x)), Null) toSeq
            case None => elem
          }
        }
        case other => other
      }
    }
    val transformer = new RuleTransformer(rr)
    nodeSeq map { transformer(_) }
  }
}
