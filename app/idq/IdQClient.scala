package idq

/*
 * Copyright (c) 2017 inBay Technologies Inc. All rights reserved.
 */


import java.util.UUID

import play.api.libs.ws.{WS, WSResponse}

import scala.concurrent.Future
import play.api.Play.current
import play.mvc.Http

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * idQ Service OAUTH API Implementation
  * implements API version v1
  *
  * @constructor Create a new idQ Service with a base URL, and OAuth registration information.
  * @param baseURL the idQ Services base URL, usually https://host/idqoauth
  * @param clientId the registered client id of the web application
  * @param clientSecret the registered client secret of the web application
  * @param redirectUri the URL on this web application to which the user will be sent back after
  *                    completing the OAuth logon process
  *
  */
class IdQClient(baseURL: String, clientId: String, clientSecret: String, redirectUri: String) {

  /* idQ Service Endpoints as defined in API v1 documentation */
  final val ROOT_URL = if (baseURL.endsWith("/")) baseURL.substring(0,baseURL.length -1) else baseURL
  final val REGISTER_PUSH_URL = ROOT_URL + "/api/v1/push"
  final val PUSH_AUTH_URL = ROOT_URL + "/api/v1/pauth"
  final val SCAN_AUTH_URL = ROOT_URL + "/api/v1/auth"
  final val OAUTH_TOKEN_URL = ROOT_URL + "/api/v1/token"
  final val OAUTH_RESULT_URL = ROOT_URL + "/api/v1/user"

  /* idQ Constants */
  final val CLIENT_ID = "client_id"
  final val CLIENT_SECRET = "client_secret"
  final val REDIRECT_URI = "redirect_uri"
  final val CODE = "code"
  final val ACCESS_TOKEN = "access_token"
  final val GRANT_TYPE = "grant_type"
  final val GRANT_TYPE_AUTH_CODE = "authorization_code"
  final val TITLE = "title"
  final val MESSAGE = "message"
  final val PUSH_ID = "push_id"
  final val TARGET = "target"
  final val PUSH_TOKEN = "push_token"
  final val USERNAME = "username"
  final val EMAIL = "email"
  final val RESOPNSE_CODE = "response_code"
  final val DESCRIPTION = "description"


  /**
    * Delegated Authorization
    * @param target The idQ ID of the user who shall receive the
    *               Delegated Authorization Request
    * @param title Detailed delegated authorization request title
    * @param message Detailed delegated authorization request message
    * @param pushId An identifier issued by the registered application
    *               that uniquely identifies this delegated authorization request
    * @return Future[WSResponse]
    */
  def requestDelegatedAuth(target: String, title: String, message: String, pushId: String) : Future[WSResponse] = {
    val response = WS.url(REGISTER_PUSH_URL)
      .withHeaders(Http.HeaderNames.CONTENT_TYPE -> "application/x-www-form-urlencoded")
      .post(Map(
        CLIENT_ID -> Seq(clientId),
        CLIENT_SECRET -> Seq(clientSecret),
        TITLE -> Seq(title),
        MESSAGE -> Seq(message),
        TARGET -> Seq(target),
        PUSH_ID -> Seq(pushId))).map(r => r)
    response
  }


  /**
    * Build Delegated Authorization URL
    * @param pushId An identifier issued by the registered application
    *               that uniquely identifies this delegated authorization request
    * @param pushToken The delegated authorization access token returned
    *                  by the idQ Service
    * @return
    */
  def buildDeleAuthURL(pushId: String, pushToken: String): String = {
    val targetURL: String = Seq(PUSH_AUTH_URL,"?client_id=", clientId, "&push_token=", pushToken,
      "&response_type=code", "&scope=optional", "&state=", pushId, "&redirect_uri=", redirectUri).mkString
    targetURL
  }

  /**
    * Handle authorization_code and state. Exchange authorization_code for access_token
    * @param state An opaque value used by the client to maintain
    *              state between the request and callback. The authorization
    *              server includes this value when redirecting the user-agent
    *              back to the client.
    * @param code The authorization grant code returned by the idQ Service
    * @return
    */
  def handleCallback(state: String, code: String) : Future[WSResponse] = {
    val response : Future[WSResponse]= WS.url(OAUTH_TOKEN_URL)
      .withHeaders(Http.HeaderNames.CONTENT_TYPE -> "application/x-www-form-urlencoded")
      .post(Map(
        CLIENT_ID -> Seq(clientId),
        CLIENT_SECRET -> Seq(clientSecret),
        CODE -> Seq(code),
        REDIRECT_URI -> Seq(redirectUri),
        GRANT_TYPE -> Seq(GRANT_TYPE_AUTH_CODE))).map(r => r)
    response
  }

  /**
    * Fetch resource. Exchange access_token for user information
    * @param resp The response from handleCallback method
    * @return
    */
  def fetchResource(resp: WSResponse) : Future[WSResponse] = {
    resp.status match {
      case 200 => WS.url(OAUTH_RESULT_URL + "?access_token=" + (resp.json \ ACCESS_TOKEN).as[String]).get()
      case _ => Future.apply(resp)
    }
  }

  /**
    * Build Authentication URL
    * @param state An opaque value used by the client to maintain
    *              state between the request and callback. The authorization
    *              server includes this value when redirecting the user-agent
    *              back to the client.
    * @return
    */
  def buildAuthURL(state: String) : String = {
    val targetURL: String = Seq(SCAN_AUTH_URL,"?client_id=", clientId,
      "&response_type=code", "&scope=optional", "&state=", state, "&redirect_uri=", redirectUri).mkString
    targetURL
  }

  /**
    * Generate Random String
    * @return
    */
  def randomString() : String = {
    UUID.randomUUID().toString
  }

}