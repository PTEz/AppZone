package jp.cyberagent.appzone

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.record.field.StringField
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field.BsonRecordField
import net.liftweb.mongodb.record.BsonRecord
import net.liftweb.mongodb.record.BsonMetaRecord
import net.liftweb.record.field.BooleanField
import net.liftweb.record.field.IntField
import net.liftweb.mongodb.record.field.DateField
import java.util.Date
import net.liftweb.json.Serializer
import net.liftweb.json.Formats
import net.liftweb.json.TypeInfo
import net.liftweb.json.JValue
import net.liftweb.json.JObject
import net.liftweb.json.JField
import net.liftweb.json.JString
import net.liftweb.json.MappingException
import net.liftweb.mongodb.Meta
import net.liftweb.json.DateFormat
import jp.cyberagent.appzone.field.BsonRecordMapField
import net.liftweb.mongodb.record.field.MongoMapField
import net.liftweb.common.Box
import scala.collection.immutable.Map

case class App private () extends MongoRecord[App] {
  def meta = App

  object id extends StringField(this, 20)
  object name extends StringField(this, 20)
  object description extends StringField(this, 255) { override def optional_? = true }

  object android extends ReleaseMap(this)
  object ios extends ReleaseMap(this)
}

object App extends App with MongoMetaRecord[App]

/////////////////////
class ReleaseMap(rec: App) extends BsonRecordMapField[App, AppPlatformEntry](rec, AppPlatformEntry) {
  override def defaultValue = Map[String, AppPlatformEntry]()
  def addApp(releaseId: String, record: AppPlatformEntry) {
    this.set(this.value + (keyifyId(releaseId) -> record))
  }
  def getApp(releaseId: String): Box[AppPlatformEntry] = {
    Box(this.value.get(keyifyId(releaseId)))
  }
  def keyifyId(id: String) = id.replace(".", "_");
}
/////////////////////
class AppPlatformEntry private () extends BsonRecord[AppPlatformEntry] {
  def meta = AppPlatformEntry

  object version extends StringField(this, 255)
  object versionCode extends IntField(this, 0)
  object lastUpdateDate extends StringField(this, 24)
  object changelog extends StringField(this, 2000)

  object releaseName extends StringField(this, 50) { override def optional_? = true }
  object releaseNotes extends StringField(this, 1024) { override def optional_? = true }

  def setDateToNow() = lastUpdateDate.set(AppPlatformEntry.DATE_FORMAT.format(new Date))
  def incrementVersionCode() = versionCode.set(versionCode.get + 1)
  def addChangeLog(change: String) {
    var newChangeLog = "[" + versionCode.get + "]\n" + change
    if (changelog.get.length() > 0)
      newChangeLog = newChangeLog + "\n" +changelog.get
      if (newChangeLog.length() > 2000) 
        newChangeLog = newChangeLog.substring(0, 1999)
    changelog.set(newChangeLog)
  }
}
object AppPlatformEntry extends AppPlatformEntry with BsonMetaRecord[AppPlatformEntry] {
  val DATE_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
}