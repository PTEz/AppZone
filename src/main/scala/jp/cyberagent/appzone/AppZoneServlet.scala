package jp.cyberagent.appzone

import org.scalatra._
import scalate.ScalateSupport
import org.scalatra.servlet.FileUploadSupport
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.liftweb.mongodb.MongoDB
import net.liftweb.mongodb.DefaultMongoIdentifier
import net.liftweb.mongodb.Upsert
import com.mongodb._
import com.mongodb.BasicDBObjectBuilder
import java.io.FileWriter
import java.io.FileOutputStream
import java.io.FileOutputStream
import java.io.File
import com.mongodb.gridfs.GridFS

class AppZoneServlet extends ScalatraServlet with ScalateSupport with JsonHelpers with FileUploadSupport {

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

  get("/app/:id") {
    val res = App.find(("id" -> params("id")))
    if (res.isEmpty)
      resourceNotFound()
    else
      Json(res.get.asJValue)
  }

  // TODO add /app/:id/android/dev
  post("/app/:id/android") {
    fileParams.get("apk") match {
      case Some(file) =>
        val input = file.getInputStream

        MongoDB.use(DefaultMongoIdentifier) { db =>
          val fs = new GridFS(db)
          val fileName = params("id") + "/android.apk"
          fs.remove(fileName)
          val inputFile = fs.createFile(input)
          inputFile.setFilename(fileName)
          inputFile.setContentType(file.contentType.getOrElse("application/octet-stream"))
          inputFile.save
        }
                
        val query = BasicDBObjectBuilder.start
          .append("id", params("id")).get
        val update = BasicDBObjectBuilder.start
          .append("$set", BasicDBObjectBuilder.start
            .append("develop.android", true).get).get
        App.update(query, update)
        
        Json(App.find(("id" -> params("id"))).get.asJValue)
      case None =>
        BadRequest()
    }
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
