package jp.cyberagent.appzone

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.record.field.StringField
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field.BsonRecordField
import net.liftweb.mongodb.record.BsonRecord
import net.liftweb.mongodb.record.BsonMetaRecord
import net.liftweb.record.field.BooleanField

case class App private() extends MongoRecord[App] {
  def meta = App

  object id extends StringField(this, 15)
  object name extends StringField(this, 20)
  object develop extends BsonRecordField(this, DeployEntry)
  // TODO maybe use { override def optional_? = true } ?
  object stable extends BsonRecordField(this, DeployEntry)
}

object App extends App with MongoMetaRecord[App]

////////////////////

class DeployEntry private () extends BsonRecord[DeployEntry] {
  def meta = DeployEntry

  object android extends BooleanField(this, false)
  object ios extends BooleanField(this, false)
}
object DeployEntry extends DeployEntry with BsonMetaRecord[DeployEntry]