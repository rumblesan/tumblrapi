package com.rumblesan.tumblr.api

import org.scribe.builder._
import org.scribe.builder.api._
import org.scribe.model._
import org.scribe.oauth._

import com.codahale.jerkson.Json._

import scala.collection.JavaConversions._

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
  val apiBase    = "http://api.tumblr.com"
  val apiVersion = "v2"
  val apiUrl     = "%s/%s".format(apiBase, apiVersion)

  def addQueryParams(request:OAuthRequest, params:Map[String,String]) = {
    params.foldLeft(request)(
      (request, keyVal) => {
        request.addQuerystringParameter(keyVal._1, keyVal._2)
        request
      }
    )
  }

  def addBodyParams(request:OAuthRequest, params:Map[String,String]) {
    params.foldLeft(request)(
      (request, keyVal) => {
        request.addBodyParameter(keyVal._1, keyVal._2)
        request
      }
    )
  }

  def uploadFile(request:OAuthRequest, params:Map[String,String], fileData:Array[Byte]) = {
    val boundary     = generateBoundaryString()
    val sectionStart = "--%s\r\n".format(boundary)

    val formData = params.foldLeft("")(
      (formData, keyVal) => {
        val (fieldName, fieldValue) = keyVal

        formData ++
        sectionStart ++
        """Content-Disposition: form-data; name="%s"""".format(fieldName) ++
        "\r\n" ++
        "Content-Type: text/plain\r\n\r\n" ++
        fieldValue ++
        "\r\n"
      }
    )

    // For files, the field name is always data
    val fileForm = sectionStart ++
                   """Content-Disposition: form-data; name="%s"; """.format("data") ++
                   """filename="%s"""".format("upload.jpeg") ++
                   "\r\n" ++
                   "Content-Type: image/jpeg\r\n\r\n"

    val bodyData = formData.getBytes ++
                   fileForm.getBytes ++
                   fileData ++
                   "\r\n--%s--\r\n".format(boundary).getBytes

    request.addPayload(bodyData)

    request.addHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
    request.addHeader("Content-Length", bodyData.length.toString)

    request.send().getBody()
  }

  def generateBoundaryString() = {
    "THISISTHEBOUNDaRYSTRINGITSCRAPFORTHEMOMENTERGERERGERGERG"
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
                 fileData:Array[Byte] = Array.empty[Byte]) = {

    val url = if (blogUrl.isEmpty) {
      "%s/%s".format(TumblrAPI.apiUrl, endpoint)
    } else {
      "%s/blog/%s/%s".format(TumblrAPI.apiUrl, blogUrl, endpoint)
    }

    val reqParams = defaultParams ++ params

    method match {
      case "GET" => {

        val request = new OAuthRequest(Verb.GET, url)
        TumblrAPI.addQueryParams(request, reqParams)
        service.signRequest(accessToken, request)

        val response = request.send()
        response.getBody()
      }
      case "POST" => {
        val request = new OAuthRequest(Verb.POST, url)
        TumblrAPI.addBodyParams(request, reqParams)
        service.signRequest(accessToken, request)

        // If there is a file to upload then shenanigans need to happen
        // We need to take the signed request params amd use them with
        // a new request, but one with the params and data as multipart
        // form data
        if (fileData.length != 0) {
          TumblrAPI.uploadFile(request, reqParams, fileData)
        } else {
          val response = request.send()
          response.getBody()
        }

      }
      case _ => {
        "Not Supported"
      }
    }

  }

}

