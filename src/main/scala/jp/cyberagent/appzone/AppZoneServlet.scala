package jp.cyberagent.appzone

import org.scalatra._
import scalate.ScalateSupport
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.liftweb.mongodb.MongoDB
import net.liftweb.mongodb.DefaultMongoIdentifier
import net.liftweb.mongodb.Upsert
import com.mongodb._

class AppZoneServlet extends ScalatraServlet with ScalateSupport with JsonHelpers {

  MongoDB.defineDb(DefaultMongoIdentifier, new Mongo, "appzone")

  get("/apps") {
    Json(App.findAll.map(p => p.asJValue))
  }

  post("/app") {
    val app = App.createRecord
    app.id.set(params.get("id").getOrElse(""))
    app.name.set(params.get("name").getOrElse(""))
    App.update(("id" -> app.id.asJValue), app, Upsert)
    Json(app.asJValue)
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
