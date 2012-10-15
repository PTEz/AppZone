package jp.cyberagent.appzone

import org.scalatra._
import org.scalatra.servlet.FileUploadSupport
import scalate.ScalateSupport
import net.liftweb.common.Empty
import net.liftweb.common.Full
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.liftweb.mongodb.MongoDB
import net.liftweb.mongodb.DefaultMongoIdentifier
import net.liftweb.mongodb.Upsert
import com.mongodb._
import com.mongodb.BasicDBObjectBuilder
import com.mongodb.gridfs.GridFS
import java.io.FileWriter
import java.io.FileOutputStream
import java.io.File
import java.util.Date

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

  get("/app/:id/android") {
    MongoDB.use(DefaultMongoIdentifier) { db =>
      val fs = new GridFS(db)
      val appId = params("id")
      val file = fs.findOne(appId + "/android.apk")
      if (file != null) {
        response.setHeader("Content-Type", "application/vnd.android.package-archive")
        response.setHeader("Content-Disposition", "attachment; filename=\"" + appId + ".apk\"")
        response.setHeader("Content-Length", file.getLength().toString)
        org.scalatra.util.io.copy(file.getInputStream(), response.getOutputStream)
      } else {
        resourceNotFound()
      }
    }
  }

  post("/app/:id/android") {
    val appId = params("id")
    fileParams.get("apk") match {
      case Some(file) =>
        val input = file.getInputStream
        MongoDB.use(DefaultMongoIdentifier) { db =>
          val fs = new GridFS(db)
          val fileName = appId+ "/android.apk"
          fs.remove(fileName)
          val inputFile = fs.createFile(input)
          inputFile.setFilename(fileName)
          inputFile.setContentType(file.contentType.getOrElse("application/octet-stream"))
          inputFile.save
        }
        val appRes = App.find(("id" -> appId))
        appRes match {
          case Full(app) => {
            val record: AndroidEntry = app.android.valueBox.openOr(AndroidEntry.createRecord)
            record.version.set(params.getOrElse("version", "NOT SET"))
            record.incrementVersionCode
            record.setDateToNow
            app.android.set(record)
            App.update(("id" -> appId), app)
            Json(app.asJValue)
          }
          case _ => resourceNotFound()
        }
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
