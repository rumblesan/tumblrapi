package com.rumblesan.tumblr.api

object App {
  def main(args: Array[String]) {
    val apiKey    = ""
    val apiSecret = ""

    val tumblrAuth = new TumblrAuthenticate(apiKey, apiSecret)
    tumblrAuth.getAccessToken()
  }
}
