package jp.cyberagent.appzone

import org.scalatra._
import scalate.ScalateSupport
import com.mongodb.casbah.Imports._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

class AppZoneServlet extends ScalatraServlet with ScalateSupport with JsonHelpers{

  val apps = MongoConnection()("appzone")("apps")

  get("/") {
    val builder = MongoDBObject.newBuilder
    builder += "foo" -> "bar"
    builder += "x" -> "y"
    builder += ("pie" -> 3.14)
    builder += ("spam" -> "eggs", "mmm" -> "bacon")
    val newObj = builder.result
    apps += newObj

    Json(new App("hello"))
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    findTemplate(requestPath) map { path =>
      contentType = "text/html"
      layoutTemplate(path)
    } orElse serveStaticResource() getOrElse resourceNotFound()
  }
}
