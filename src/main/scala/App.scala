package com.rumblesan.tumblr.api

import com.codahale.jerkson.Json._
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

object App {
  def main(args: Array[String]) {

    val source = scala.io.Source.fromFile("config.cfg")
    val lines = source.mkString
    source.close()

    val cfg = parse[Map[String,String]](lines)

    val image = ImageIO.read(new File("landscape.jpeg"))
    val baos = new ByteArrayOutputStream()
    ImageIO.write(image, "jpg", baos)
    baos.flush()
    val imageData = baos.toByteArray()
    baos.close()

    val tumblrApi = new TumblrAPI(cfg("apiKey"), cfg("apiSecret"), cfg("oauthToken"), cfg("oauthSecret"))
    val info = tumblrApi.apiRequest("followers", "rumblesan.tumblr.com")
    println(info)

    val params = Map("type" -> "photo", "caption" -> "Just testing. Please ignore", "tags" -> "testing")
    val response = tumblrApi.apiRequest("post", "rumblesan.tumblr.com", "POST", params, imageData)
    println(response)

  }
}
