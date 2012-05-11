package de.dimond.tippspiel.util

import java.io.InputStream
import java.net._

object NetHelpers {
  def httpGet[T](url: String, f: InputStream => T): T = {
    new URL(url).openConnection match {
      case conn: HttpURLConnection => {
        conn.connect()
        val result = f(conn.getInputStream())
        conn.disconnect()
        result
      }
    }
  }

  def httpDelete(url: String): Int = httpDelete(url, _ => ())._1

  def httpDelete[T](url: String, f: InputStream => T): (Int, T) = {
    new URL(url).openConnection match {
      case conn: HttpURLConnection => {
        conn.setDoOutput(true)
        conn.setRequestMethod("DELETE")
        /* Set content length to 0 as Facebook will not accept requests with non-zero content length */
        conn.setFixedLengthStreamingMode(0)
        conn.connect()
        val code = conn.getResponseCode
        val res = if (code < 400) f(conn.getInputStream()) else f(conn.getErrorStream())
        conn.disconnect()
        (code, res)
      }
    }
  }
}
