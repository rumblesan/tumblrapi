package com.rumblesan.util.tumblrapi

import org.scribe.builder.ServiceBuilder
import org.scribe.builder.api.TumblrApi
import org.scribe.model.{Token, OAuthRequest, Verifier, Verb}
import org.scribe.exceptions._

import com.typesafe.config.ConfigFactory

import java.util.{UUID, Scanner}

/** This is the TumblrAPI companion object
  *
  * It hold methods to simplify adding various parameters to a request or
  * parsing the reponses from the API
  */
object TumblrAPI {

  val config = ConfigFactory.load()

  val apiBase    = config.getString("api.tumblr.url")
  val apiVersion = config.getString("api.tumblr.version")
  val apiUrl     = "http://%s/%s".format(apiBase, apiVersion)

  /** Easily adds query parameters to an OAuthRequest class
    *
    * @param request The OAuthRequest object the params are to be added to
    * @param params A ''Map'' of ''String'' keys and values to add as query
    *               paramaters to the request
    * @return This method is entirely side effects and so returns Unit
    */
  def addQueryParams(request:OAuthRequest, params:Map[String,String]):Unit = {
    params.foldLeft(request)(
      (request, keyVal) => {
        request.addQuerystringParameter(keyVal._1, keyVal._2)
        request
      }
    )
  }

  /** Easily adds body parameters to an OAuthRequest class
    *
    * @param request The ''OAuthRequest'' object the params are to be added to
    * @param params A ''Map'' of ''String'' keys and values to add as body
    *               paramaters to the request
    * @return This method is entirely side effects and so returns ''Unit''
    */
  def addBodyParams(request:OAuthRequest, params:Map[String,String]):Unit = {
    params.foldLeft(request)(
      (request, keyVal) => {
        request.addBodyParameter(keyVal._1, keyVal._2)
        request
      }
    )
  }

  /** Adds Multi-Part Form data to the request
    *
    * This is used primarily for file uploads. In this situation Tumblr
    * expects the file data to be attached to the request as form data
    * with the field name '''data'''. The rest of the paramaters must also
    * be included as form data, but not any OAuth parameters.
    *
    * @param request The ''OAuthRequest'' object to add the data to
    * @param params A Map of String keys and values for creating the form data
    * @param fileData The binary file data to be included
    * @return This method is entirely side effects and so returns ''Unit''
    */
  def addMultiPartFormParams(request:OAuthRequest,
                             params:Map[String,String],
                             fileData:Array[Byte],
                             fileFormat: String):Unit = {
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
                   """filename="upload.%s"""".format(fileFormat) ++
                   "\r\n" ++
                   "Content-Type: image/%s\r\n\r\n".format(fileFormat)

    val bodyData = formData.getBytes ++
                   fileForm.getBytes ++
                   fileData ++
                   "\r\n--%s--\r\n".format(boundary).getBytes

    request.addPayload(bodyData)

    request.addHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
    request.addHeader("Content-Length", bodyData.length.toString)
  }

  /** Generates a random boundary string to use with the form data
    * @return The boundary string
    */
  def generateBoundaryString():String = {
    UUID.randomUUID.toString
  }

}

/** The main TumblrAPI class
  *
  * This is essentially just a wrapper around the '''Scribe''' library
  * and is meant to contain all the logic and parsing necessary to interact
  * with the Tumblr API.
  *
  * @param apiKey the api key used for OAuth
  * @param apiSecret the api secret used for OAuth
  * @param oauthToken the token part of the OAuth access token
  * @param oauthSecret the secret part of the OAuth access token
  */
class TumblrAPI(apiKey:String, apiSecret:String, oauthToken:String, oauthSecret:String) {

  /** The OAuth access token used to sign requests
    *
    * needs the oauthToken and oauthSecret strings
    */
  val accessToken = new Token(oauthToken, oauthSecret)

  /** The OAuth service object
    *
    * Contains all the OAuth specific behaivour
    */
  val service = new ServiceBuilder()
                    .provider(classOf[TumblrApi])
                    .apiKey(apiKey)
                    .apiSecret(apiSecret)
                    .build()

  /** The default parameters to add to every request
    */
  val defaultParams = Map("api_key" -> apiKey)

  /** Generates an OAuth request token from the OAuth service object
    *
    * This is used when first authenticating the the OAuth API
    *
    * @return The oauth ''Token'' object
    */
  def getRequestToken():Token = {
    service.getRequestToken()
  }

  /** Generates an Authorization URL for the given request token
    *
    * @param requestToken The request token to generate an auth URL for
    * @return The auth URL to go to
    */
  def getAuthorizationUrl(requestToken:Token):String = {
    service.getAuthorizationUrl(requestToken)
  }

  /** Generates an OAuth access token from the given request token and 
    * verification string
    *
    * @param requestToken The request token used to generate the auth URL
    * @param verificationString The verification string generated by Tumblr
    * @return the OAuth access token
    */
  def getAccessToken(requestToken:Token, verifcationString:String):Token = {
      val verifier = new Verifier(verifcationString)
      service.getAccessToken(requestToken, verifier)
  }

  /** This is the method that actually creates the OAuth request and sends it
    *
    * @todo This could probably do with being more robust. These a bunch of
    *       Exceptions I don't think I check for.
    * @param endpoint the API endpoint to query
    * @param blogUrl the URL of the blog to get information from. If this is
    *                a blank string then it's assumed to be your own blog
    * @param method The HTTP method used for the API call.
    *               Supports '''GET''' or '''POST'''
    * @param params a ''Map'' of ''Strings'' containing any extra parameters
    *               to use with the query
    * @param fileData the binary data of the file to be uploaded, if any
    * @return an ''Option'' class containing a ''String'' if the request
    *         was successful, None otherwise
    */
  def apiRequest(endpoint:String,
                 blogUrl:String = "",
                 method:String = "GET",
                 params:Map[String,String] = Map.empty[String,String],
                 fileData:Array[Byte] = Array.empty[Byte],
                 fileFormat: String = "jpeg"
                 ):Option[String] = {

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
          TumblrAPI.addMultiPartFormParams(request, reqParams, fileData, fileFormat)
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

  /** Wraps the apiRequest method for GET requests and parses the response JSON
    *
    * @param endpoint the API endpoint to query
    * @param blogUrl the URL of the blog to get information from. If this is
    *                a blank string then it's assumed to be your own blog
    * @param params a ''Map'' of ''Strings'' containing any extra parameters
    *               to use with the query
    * @return an ''Option'' class containing a ''TumblrApiResponse'' case
    *         class if the request was successful, None otherwise
    */
  def get(endpoint:String,
          blogUrl:String = "",
          params:Map[String,String] = Map.empty[String,String]):Option[String] = {

    apiRequest(endpoint, blogUrl, "GET", params)
  }

  /** Wraps the apiRequest method for POST requests and parses the response JSON
    *
    * @param endpoint the API endpoint to query
    * @param blogUrl the URL of the blog to get information from. If this is
    *                a blank string then it's assumed to be your own blog
    * @param params a ''Map'' of ''Strings'' containing any extra parameters
    *               to use with the query
    * @param fileData the binary data of the file to be uploaded, if any
    * @return an ''Option'' class containing a ''String'' if the request
    *         was successful, None otherwise
    */
  def post(endpoint:String,
           blogUrl:String = "",
           params:Map[String,String] = Map.empty[String,String],
           fileData:Array[Byte] = Array.empty[Byte],
           fileFormat: String = "jpeg"
           ):Option[String] = {

    apiRequest(endpoint, blogUrl, "POST", params, fileData, fileFormat)
  }

}

