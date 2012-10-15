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

case class App private() extends MongoRecord[App] {
  def meta = App

  object id extends StringField(this, 15)
  object name extends StringField(this, 20)
  object description extends StringField(this, 255) { override def optional_? = true }

  object android extends BsonRecordField(this, AppPlatformEntry) { override def optional_? = true }
  object ios extends BsonRecordField(this, AppPlatformEntry) { override def optional_? = true }
}

object App extends App with MongoMetaRecord[App]

/////////////////////
class AppPlatformEntry private() extends BsonRecord[AppPlatformEntry] {
  def meta = AppPlatformEntry
  
  object version extends StringField(this, 255)
  object versionCode extends IntField(this, 0)
  object lastUpdateDate extends StringField(this, 24)
  
  def setDateToNow() = lastUpdateDate.set(AppPlatformEntry.DATE_FORMAT.format(new Date))
  def incrementVersionCode() = versionCode.set(versionCode.get + 1)
}
object AppPlatformEntry extends AppPlatformEntry with BsonMetaRecord[AppPlatformEntry] {
  val DATE_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
}