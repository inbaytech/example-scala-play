package controllers

import java.util.{Calendar}
import javax.inject._

import com.typesafe.config.ConfigFactory
import idq.IdQClient
import play.api._
import play.api.mvc._



/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject() extends Controller {
  val conf = ConfigFactory.load()
  val baseUrl = conf.getString("oauth.base_url")
  val clientId = conf.getString("oauth.client_id")
  val clientSecret = conf.getString("oauth.client_secret")
  val redirectUrl = conf.getString("oauth.redirect_url")
  val idq = new IdQClient(baseUrl, clientId, clientSecret, redirectUrl)
  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  /**
    * Initial authentication
    * @return
    */
  def authenticate = Action {
    Redirect(idq.buildAuthURL(idq.randomString()))
  }


  /**
    * Handle returned authorization_code and state from idQ Service
    * after person finished authentication
    * @param state An unique value used by the client to identity
    *              the callback is from the requested server
    * @param code The authorization grant code returned by the idQ Service
    * @return
    */
  def handleCallback(state: String, code: String) = Action.async{ implicit request =>
    import scala.concurrent.ExecutionContext.Implicits.global
    val response = for {
      resp <- idq.handleCallback(state, code)
      resp <- idq.fetchResource(resp)
    }yield{ resp }

    response.recover{
      case e: Exception =>
        InternalServerError("InternalServerError when handling redirect")
    }

    response.map(resp => {
      resp.status match {
        case 200 =>
          val idqid = (resp.json \ idq.USERNAME).as[String]
          val idqemail = (resp.json \ idq.EMAIL).as[String]
          val responseCode : Option[String] = (resp.json \ idq.RESOPNSE_CODE).asOpt[String]
          val description : Option[String] = (resp.json \ idq.DESCRIPTION).asOpt[String]
          // Now your web application gets user information.
          // In this example, it return user information to a view,
          // but you can link user to your business logic.
          Ok(views.html.dashboard(idqid, idqemail, responseCode, description))
        case _ =>
          Status(resp.status)(resp.body)
      }
    })
  }

  /**
    * Handle delegated authorization request
    * @param target The idQ ID of the user who shall receive the
    *               Delegated Authorization Request
    * @return
    */
  def handleDelegatedAuthorization(target: String) = Action.async { implicit request =>
    import scala.concurrent.ExecutionContext.Implicits.global

    val push_id = (System.currentTimeMillis + (Math.random() * 1000)).toString
    // Sample title and message
    val title = "Authorization Request"
    val message = "You did submit a transfer request: " +
      "Transfer $1000 to John from your account 00576-1000038 at " +
      Calendar.getInstance().getTime + ". Approve?"
    val response = idq.requestDelegatedAuth(target, title, message, push_id)
    response.map(resp => {
      Logger.debug(resp.body)
      resp.status match {
        case 200 => Redirect(idq.buildDeleAuthURL(push_id, (resp.json \ idq.PUSH_TOKEN).as[String]))
        case 400 => Status(resp.status)(resp.body)
      }
    })
  }
}
