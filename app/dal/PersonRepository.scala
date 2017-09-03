package dal

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import javax.inject.Inject
import javax.inject.Singleton
import models.Person
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.Await
import scala.concurrent.duration.Duration


/**
 * A repository for people.
 *
 * @param dbConfigProvider The Play db config provider. Play will inject this for you.
 */
@Singleton
class PersonRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  /**
   * Here we define the table. It will have a name of people
   */
  private class PeopleTable(tag: Tag) extends Table[Person](tag, "people") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    /** The name column */
    def name = column[String]("name")

    /** The age column */
    def age = column[Int]("age")

    /**
     * This is the tables default "projection".
     *
     * It defines how the columns are converted to and from the Person object.
     *
     * In this case, we are simply passing the id, name and page parameters to the Person case classes
     * apply and unapply methods.
     */
    def * = (id, name, age) <> ((Person.apply _).tupled, Person.unapply)
  }

  /**
   * The starting point for all queries on the people table.
   */
  private val people = TableQuery[PeopleTable]

  /**
   * Create a person with the given name and age.
   *
   * This is an asynchronous operation, it will return a future of the created person, which can be used to obtain the
   * id for that person.
   */
  def create(name: String, age: Int): Future[Person] = db.run {
    // We create a projection of just the name and age columns, since we're not inserting a value for the id column
    (people.map(p => (p.name, p.age))
      // Now define it to return the id, because we want to know what id was generated for the person
      returning people.map(_.id)
      // And we define a transformation for the returned value, which combines our original parameters with the
      // returned id
      into ((nameAge, id) => Person(id, nameAge._1, nameAge._2))
    // And finally, insert the person into the database
    ) += (name, age)
  }
  
  def insert = {
    val models:Seq[(String,Int)] = Seq(("Test4",10),("Test5",11),("Test6",12))
    
    val insertAction = DBIO.seq(people.map(p => (p.name, p.age)) ++= models)
    
    val futureData = db.run(insertAction.transactionally.asTry)
    
    futureData.onComplete{
      case Success(results) => Logger.info(results.toString)
      case Failure(t) => Logger.info(t.getMessage())
    }
  }
  
  def select = {
		  val q = people.map(_.name)
			val action = q.result
			val futureData: Future[Seq[String]] = db.run(action)

			futureData.onComplete {
		    case Success(results) => Logger.info(results.toString)
		    case Failure(t)   => Logger.info(t.getMessage())
		  }
  }
  
  def multipleUpdate = {
    val models:Seq[Person] = Seq(Person(1,"up1",40),Person(99,"up2",41),Person(3,"up3",42))
    val updateAction = DBIO.seq(models.map(p =>{ people.filter(_.id === p.id).update(p) }): _*)
    val futureData = db.run(updateAction.transactionally.asTry)
    
    //Await.result(futureData,Duration("10"))
    
    futureData.onComplete{
      case Success(results) => Logger.info(results.toString)
      case Failure(t) => Logger.info(t.getMessage())
    }
  }
  
  def update = {
    val q = for { c <- people if c.name === "James" } yield (c.name, c.age)
    val updateAction = q.update("Honda",13)
    // Get the statement without having to specify an updated value:
    val sql = q.updateStatement
    Logger.info(sql)
    
    val futureData = db.run(updateAction)
    futureData.onComplete {
		    case Success(results) => Logger.info(results.toString)
		    case Failure(t)   => println(t.getMessage())
		 }
    
  }

  /**
   * List all the people in the database.
   */
  def list(): Future[Seq[Person]] = db.run {
    people.result
  }
}
