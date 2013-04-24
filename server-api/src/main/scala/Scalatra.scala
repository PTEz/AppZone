import jp.co.cyberagent.appzone._
import org.scalatra._
import javax.servlet.ServletContext
import com.mongodb.Mongo
import net.liftweb.mongodb.MongoDB
import net.liftweb.mongodb.DefaultMongoIdentifier
import com.mongodb.ServerAddress
import net.liftweb.util.Props

/**
 * This is the Scalatra bootstrap file. You can use it to mount servlets or
 * filters. It's also a good place to put initialization code which needs to
 * run at application start (e.g. database configurations), and init params.
 */
class Scalatra extends LifeCycle {
  override def init(context: ServletContext) {
    configureMongoDb
    // Mount one or more servlets
    // context.mount(new AppZoneServlet, "/*")
  }

  def configureMongoDb = {
    val srvr = new ServerAddress(
      Props.get("mongo.host", "127.0.0.1"),
      Props.getInt("mongo.port", 27017))
    val db = Props.get("mongo.db", "appzone")
    MongoDB.defineDb(DefaultMongoIdentifier, new Mongo(srvr), db)
  }
}
