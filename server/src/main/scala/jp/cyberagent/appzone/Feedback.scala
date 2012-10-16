package jp.cyberagent.appzone

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.record.field.StringField
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field.ObjectIdPk
import java.util.Date

case class Feedback private() extends MongoRecord[Feedback] with ObjectIdPk[Feedback] {
  def meta = Feedback

  object appId extends StringField(this, 20)
  object appType extends StringField(this, 10)
  object feedback extends StringField(this, "")
  object date extends StringField(this, Feedback.DATE_FORMAT.format(new Date))
}

object Feedback extends Feedback with MongoMetaRecord[Feedback] {
  val DATE_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
}