package jp.cyberagent.appzone

import org.scalatra.test.scalatest.ScalatraSuite
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterEach
import net.liftweb.mongodb.MongoDB
import net.liftweb.util.Props
import net.liftweb.mongodb.DefaultMongoIdentifier
import net.liftweb.mongodb.Upsert
import net.liftweb.json.JsonDSL._
import com.mongodb.Mongo
import com.mongodb.ServerAddress
import com.mongodb.DBObject
import com.mongodb.BasicDBObject
import net.liftweb.json.JsonParser
import net.liftweb.json.JArray
import net.liftweb.json.JObject
import java.io.File
import scala.collection.Map

class AppZoneServletTest extends ScalatraSuite with FunSuite with BeforeAndAfterEach {
  // `MyScalatraServlet` is your app which extends ScalatraServlet
  addServlet(classOf[AppZoneServlet], "/*")
  val DEFAULT_RELEASE_ID = "_default"

  val app = App.createRecord
  app.id.set("testid")
  app.name.set("Test Name")

  val app2 = App.createRecord
  app2.id.set("testid2")
  app2.name.set("a Test Name")

  override def beforeAll() {
    super.beforeAll()
    val srvr = new ServerAddress(
      Props.get("mongo.host", "127.0.0.1"),
      Props.getInt("mongo.port", 27017))
    MongoDB.defineDb(DefaultMongoIdentifier, new Mongo(srvr), "appzone-test")
  }

  override def beforeEach() {
    super.beforeEach()
    // Remove all apps
    App.useColl(col => col.remove(new BasicDBObject()))

    // Add test app
    App.update(("id" -> app.id.asJValue), app, Upsert)
    App.update(("id" -> app2.id.asJValue), app2, Upsert)
  }

  test("GET /apps should return the apps") {
    get("/apps") {
      status should equal(200)
      val json = JsonParser.parse(body)
      json.isInstanceOf[JArray] should equal(true)
      json.asInstanceOf[JArray].values.length should equal(2)
    }
  }
  test("GET /apps should return the apps sorted") {
    get("/apps") {
      val apps = JsonParser.parse(body).asInstanceOf[JArray]
      apps(0).asInstanceOf[JObject].values("name") should equal("a Test Name")
    }
  }

  def checkAppsContainsAndroidRelease(releaseId: String) = {
    get("/apps") {
      val apps = JsonParser.parse(body).asInstanceOf[JArray]
      apps.values.length should equal(2)
      val android = apps(1).asInstanceOf[JObject].values("android").asInstanceOf[Map[String, AppPlatformEntry]]
      android.contains(releaseId) should equal(true)
      val release = android(releaseId).asInstanceOf[Map[String, Object]]
      release("versionCode") should equal(1)
    }
  }
  test("POST /app/:id/android should add the release") {
    val apkFile = new File(getClass.getResource("/android.apk").toURI())
    post("/app/testid/android", Map("version" -> "1.0"), Map("apk" -> apkFile)) {
      status should equal(200)
    }
    checkAppsContainsAndroidRelease(DEFAULT_RELEASE_ID)
  }
  test("POST /app/:id/android/:releaseId should add the release with given releaseId") {
    val releaseId = "production"
    val apkFile = new File(getClass.getResource("/android.apk").toURI())
    post("/app/testid/android/" + releaseId, Map("version" -> "1.0"), Map("apk" -> apkFile)) {
      status should equal(200)
    }
    checkAppsContainsAndroidRelease(releaseId)
  }
  test("DELETE /app/:id should delete the app") {
    delete("/app/" + app2.id.get) {
      status should equal(200)
    }
    get("/apps") {
      val apps = JsonParser.parse(body).asInstanceOf[JArray]
      apps.values.length should equal(1)
      val android = apps(0).asInstanceOf[JObject].values("id") should equal(app.id.get)
    }
  }
}