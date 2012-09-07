package com.rumblesan.tumblr.api

import com.codahale.jerkson.Json._

object App {
  def main(args: Array[String]) {

    val source = scala.io.Source.fromFile("config.cfg")
    val lines = source.mkString
    source.close()

    val cfg = parse[Map[String,String]](lines)

    val tumblrApi = new TumblrAPI(cfg("apiKey"), cfg("apiSecret"), cfg("oauthToken"), cfg("oauthSecret"))
    val info = tumblrApi.getInfo("rumblesan.tumblr.com")
    println("got the info")
    println(info)
  }
}
