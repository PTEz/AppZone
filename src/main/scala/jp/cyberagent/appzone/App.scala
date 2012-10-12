package jp.cyberagent.appzone

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.record.field.StringField
import net.liftweb.mongodb.record.MongoMetaRecord

case class App private() extends MongoRecord[App] {
  def meta = App

  object id extends StringField(this, 15)
  object name extends StringField(this, 20)
}

object App extends App with MongoMetaRecord[App]