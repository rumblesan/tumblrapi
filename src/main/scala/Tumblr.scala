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

object TumblrAPI {
  val apiBase = "http://api.tumblr.com"
  val apiVersion = "v2"
  val apiUrl = "%s/%s".format(apiBase, apiVersion)

  import java.net.URLEncoder.encode

  def encodeParams(params:Map[String,String]) = {
    params.map (
      (keyVal) => {
        encode(keyVal._1, "UTF-8") + "=" + encode(keyVal._2, "UTF-8")
      }
    ).foldLeft("")(_ + _)
  }

  def addBodyParams(request:OAuthRequest, params:Map[String,String]) {
    params.foldLeft(request)(
      (request, keyVal) => {
        request.addBodyParameter(keyVal._1, keyVal._2)
        request
      }
    )
  }

}

class TumblrAPI(apiKey:String, apiSecret:String, oauthToken:String, oauthSecret:String) {

  val accessToken = new Token(oauthToken, oauthSecret)

  val service = new ServiceBuilder()
                    .provider(classOf[TumblrApi])
                    .apiKey(apiKey)
                    .apiSecret(apiSecret)
                    .build()

  val defaultParams = Map("api_key" -> apiKey)

  def apiRequest(endpoint:String,
                 blogUrl:String = "",
                 method:String = "GET",
                 params:Map[String,String] = Map.empty[String,String],
                 files:List[String] = Nil) = {

    val url = if (blogUrl.isEmpty) {
      "%s/%s".format(TumblrAPI.apiUrl, endpoint)
    } else {
      "%s/blog/%s/%s".format(TumblrAPI.apiUrl, blogUrl, endpoint)
    }
    println(url)

    val reqParams = defaultParams ++ params

    method match {
      case "GET" => {
        val reqUrl = "%s?%s".format(url, TumblrAPI.encodeParams(reqParams))
        println(reqUrl)
        val request = new OAuthRequest(Verb.GET, reqUrl)
        service.signRequest(accessToken, request)
        val response = request.send()
        response.getBody()
      }
      case "POST" => {
        val request = new OAuthRequest(Verb.GET, url)
        TumblrAPI.addBodyParams(request, reqParams)
        service.signRequest(accessToken, request)
        val response = request.send()
        response.getBody()
      }
      case _ => {
        "Not Supported"
      }
    }

  }

}

