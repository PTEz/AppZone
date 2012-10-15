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

  object android extends BsonRecordField(this, AndroidEntry) { override def optional_? = true }
  object ios extends BsonRecordField(this, AndroidEntry) { override def optional_? = true }
}

object App extends App with MongoMetaRecord[App]

/////////////////////
class AndroidEntry private() extends BsonRecord[AndroidEntry] {
  def meta = AndroidEntry
  
  object version extends StringField(this, 255)
  object versionCode extends IntField(this, 1)
  object lastUpdateDate extends StringField(this, 24)
  
  def setDateToNow() = lastUpdateDate.set(AndroidEntry.DATE_FORMAT.format(new Date))
}
object AndroidEntry extends AndroidEntry with BsonMetaRecord[AndroidEntry] {
  val DATE_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
}