package jp.co.cyberagent.appzone

import org.scalatra.auth.strategy.{BasicAuthStrategy, BasicAuthSupport}
import org.scalatra.auth.{ScentrySupport, ScentryConfig}
import org.scalatra.{ScalatraBase}
import javax.naming.directory.DirContext
import javax.naming.directory.InitialDirContext
import javax.naming.Context
import java.util.Hashtable
import net.liftweb.util.Props
import org.slf4j.LoggerFactory
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class AppZoneBasicAuthStrategy(protected override val app: ScalatraBase, realm: String)
  extends BasicAuthStrategy[User](app, realm) {
  val logger = LoggerFactory.getLogger(getClass)

  protected def validate(userName: String, password: String): Option[User] = {
    if (loginLdap(userName, password)) Some(User(userName))
    else None
  }
  
  def doLogin(username: String, password: String): Boolean = {
    Props.get("auth.source", "ldap") match {
      case "ldap" => loginLdap(username, password)
      case _ => false
    }
  }
  
  def loginLdap(username: String, password: String): Boolean = {
    try {
			val env: Hashtable[String, String] = new Hashtable[String, String]()
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
			env.put(Context.PROVIDER_URL, Props.get("auth.ldap.url", null))

			env.put(Context.SECURITY_AUTHENTICATION, "simple")
			env.put(Context.SECURITY_PRINCIPAL, Props.get("auth.ldap.principal", null) format(username))
			env.put(Context.SECURITY_CREDENTIALS, password)

			val ctx: DirContext = new InitialDirContext(env)
			val result = ctx != null

			if (ctx != null)
				ctx.close()

			result
		} catch {
		  case e:Exception =>
		    logger.info("LDAP login failure", e)
			false
		}
  }

  protected def getUserId(user: User): String = user.id
}

trait AuthenticationSupport extends ScentrySupport[User] with BasicAuthSupport[User] {
  self: ScalatraBase =>

  val realm = "AppZone"

  protected def fromSession = { case id: String => User(id)  }
  protected def toSession   = { case usr: User => usr.id }

  protected val scentryConfig = (new ScentryConfig {}).asInstanceOf[ScentryConfiguration]

  override protected def configureScentry = {
    scentry.unauthenticated {
      scentry.strategies("Basic").unauthenticated()
    }
  }

  override protected def registerAuthStrategies = {
    scentry.register("Basic", app => new AppZoneBasicAuthStrategy(app, realm))
  }
}

case class User(id: String)