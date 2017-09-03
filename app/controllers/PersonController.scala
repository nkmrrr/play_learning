package controllers

import scala.concurrent.ExecutionContext

import dal._
import javax.inject._
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._

class PersonController @Inject()(repo: PersonRepository,
                                  cc: ControllerComponents
                                )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with I18nSupport {

  /**
   * The mapping for the person form.
   */
  val personForm: Form[CreatePersonForm] = Form {
    mapping(
      "name" -> nonEmptyText,
      "age" -> number.verifying(min(0), max(140))
    )(CreatePersonForm.apply)(CreatePersonForm.unapply)
  }

  /**
   * The index action.
   */
def index = Action { implicit request => {
	//Ok(views.html.index(personForm))
  
	repo.create("James", 18)
	Logger.info("here")
	Ok(views.html.index("Your new application is ready."))
}
}

def insert = Action {
	repo.insert
	Logger.info("here insert")
	Ok(views.html.index("Your new application is ready."))
}

def update = Action {
	repo.multipleUpdate
	Logger.info("here update")
	Ok(views.html.index("Your new application is ready."))
}

def select = Action {
	repo.select
	Logger.info("here select")
	Ok(views.html.index("Your new application is ready."))
}
  /**
   * The add person action.
   *
   * This is asynchronous, since we're invoking the asynchronous methods on PersonRepository.
   */
//  def addPerson = Action.async { implicit request =>
//    // Bind the form first, then fold the result, passing a function to handle errors, and a function to handle succes.
//    personForm.bindFromRequest.fold(
//      // The error function. We return the index page with the error form, which will render the errors.
//      // We also wrap the result in a successful future, since this action is synchronous, but we're required to return
//      // a future because the person creation function returns a future.
//      errorForm => {
//        Logger.info("err")
//        Future.successful(Ok("Person add failed"))
//      },
//      // There were no errors in the from, so create the person.
//      person => {
//        repo.create(person.name, person.age).map { _ =>
//          // If successful, we simply redirect to the index page.
//          Redirect(routes.HomeController.index)
//        }
//      }
//    )
//  }

  /**
   * A REST endpoint that gets all the people as JSON.
   */
  def getPersons = Action.async { implicit request =>
    repo.list().map { people =>
      Ok(Json.toJson(people))
    }
  }
}

/**
 * The create person form.
 *
 * Generally for forms, you should define separate objects to your models, since forms very often need to present data
 * in a different way to your models.  In this case, it doesn't make sense to have an id parameter in the form, since
 * that is generated once it's created.
 */
case class CreatePersonForm(name: String, age: Int)
