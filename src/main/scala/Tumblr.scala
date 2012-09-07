package com.rumblesan.tumblr.api

import org.scribe.builder._
import org.scribe.builder.api._
import org.scribe.model._
import org.scribe.oauth._

import com.codahale.jerkson.Json._

class TumblrAuthenticate(apiKey:String, apiSecret:String) {

    val service:OAuthService = new ServiceBuilder()
                                   .provider(classOf[TumblrApi])
                                   .apiKey(apiKey)
                                   .apiSecret(apiSecret)
                                   .build()
    def getAccessToken() = {
      import java.util.Scanner

      val requestToken = service.getRequestToken()

      println("Go to the following URL to authorize")
      println(service.getAuthorizationUrl(requestToken))
      println("Then paste the verifier back here")

      val in       = new Scanner(System.in)
      val verifier = new Verifier(in.nextLine())

      val accessToken = service.getAccessToken(requestToken, verifier)

      println("Here's the access token")
      println(accessToken)

    }

}

class TumblrAPI(apiKey:String, apiSecret:String, oauthToken:String, oauthSecret:String) {

  val accessToken = new Token(oauthToken, oauthSecret)

  val service = new ServiceBuilder()
                    .provider(classOf[TumblrApi])
                    .apiKey(apiKey)
                    .apiSecret(apiSecret)
                    .build()

  def getInfo(blogName:String):String = {
    val url = "http://api.tumblr.com/v2/blog/%s/followers".format(blogName)

    val request = new OAuthRequest(Verb.GET, url)
    service.signRequest(accessToken, request)
    val response = request.send()

    return response.getBody()
  }


}

