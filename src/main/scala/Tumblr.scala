package com.rumblesan.tumblr.api

import org.scribe.builder.ServiceBuilder
import org.scribe.builder.api.TumblrApi
import org.scribe.model.{Token, OAuthRequest, Verifier, Verb}
import org.scribe.exceptions._

import com.codahale.jerkson.Json._

import java.util.{UUID, Scanner}

object TumblrAPI {
  val apiBase    = "http://api.tumblr.com"
  val apiVersion = "v2"
  val apiUrl     = "%s/%s".format(apiBase, apiVersion)

  def addQueryParams(request:OAuthRequest, params:Map[String,String]):Unit = {
    params.foldLeft(request)(
      (request, keyVal) => {
        request.addQuerystringParameter(keyVal._1, keyVal._2)
        request
      }
    )
  }

  def addBodyParams(request:OAuthRequest, params:Map[String,String]):Unit = {
    params.foldLeft(request)(
      (request, keyVal) => {
        request.addBodyParameter(keyVal._1, keyVal._2)
        request
      }
    )
  }

  def addMultiPartFormParams(request:OAuthRequest,
                             params:Map[String,String],
                             fileData:Array[Byte]):Unit = {
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
  }

  def generateBoundaryString():String = {
    UUID.randomUUID.toString
  }

  def parseApiGetResponse(jsonData:String, postType:String = ""):Option[TumblrApiResponse] = {
    val firstParse = parse[TumblrResponse](jsonData)
    firstParse.meta.status match {
      case 404 => None
      case 200 => {
        postType match {
          case "info" => parse[TumblrInfoQueryResponse](jsonData).response
          case "photo" => parse[TumblrPhotoQueryResponse](jsonData).response
          case _ => parse[TumblrAnyQueryResponse](jsonData).response
        }
      }
      case _ => None
    }
  }

  def parseApiPostResponse(jsonData:String):Option[TumblrApiResponse] = {
    val firstParse = parse[TumblrResponse](jsonData)
    firstParse.meta.status match {
      case 404 => None
      case 201 => {
        parse[TumblrPostResponse](jsonData).response
      }
      case 200 => {
        parse[TumblrPostResponse](jsonData).response
      }
      case _ => None
    }
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

  def getRequestToken():Token = {
    service.getRequestToken()
  }

  def getAuthorizationUrl(requestToken:Token):String = {
    service.getAuthorizationUrl(requestToken)
  }

  def getAccessToken(requestToken:Token, verifcationString:String):Token = {
      val verifier = new Verifier(verifcationString)
      service.getAccessToken(requestToken, verifier)
  }

  def apiRequest(endpoint:String,
                 blogUrl:String = "",
                 method:String = "GET",
                 params:Map[String,String] = Map.empty[String,String],
                 fileData:Array[Byte] = Array.empty[Byte]):Option[String] = {

    val url = if (blogUrl.isEmpty) {
      "%s/%s".format(TumblrAPI.apiUrl, endpoint)
    } else {
      "%s/blog/%s/%s".format(TumblrAPI.apiUrl, blogUrl, endpoint)
    }

    val reqParams = defaultParams ++ params

    val request = method match {
      case "GET" => {

        val request = new OAuthRequest(Verb.GET, url)
        TumblrAPI.addQueryParams(request, reqParams)
        service.signRequest(accessToken, request)
        Some(request)
      }
      case "POST" => {
        val request = new OAuthRequest(Verb.POST, url)
        TumblrAPI.addBodyParams(request, reqParams)
        service.signRequest(accessToken, request)

        // If there is a file to upload then we need to add the file data
        // and any parameters to the request as multipart form data
        val response = if (fileData.length != 0) {
          TumblrAPI.addMultiPartFormParams(request, reqParams, fileData)
        }
        Some(request)
      }
      case _ => {
        None
      }
    }

    val response = try {
      request.map(_.send())
    } catch {
      case oce:OAuthConnectionException => None
    }

    response.filter(_.isSuccessful).map(_.getBody())
  }

  def get(endpoint:String,
          blogUrl:String = "",
          params:Map[String,String] = Map.empty[String,String]):Option[TumblrApiResponse] = {

    val apiResponse = apiRequest(endpoint, blogUrl, "GET", params)
    val postType = params.getOrElse("type", "any")
    apiResponse.flatMap(TumblrAPI.parseApiGetResponse(_, postType))
  }

  def post(endpoint:String,
           blogUrl:String = "",
           params:Map[String,String] = Map.empty[String,String],
           fileData:Array[Byte] = Array.empty[Byte]):Option[TumblrApiResponse] = {

    val apiResponse = apiRequest(endpoint, blogUrl, "POST", params, fileData)
    apiResponse.flatMap(TumblrAPI.parseApiPostResponse(_))
  }

}

