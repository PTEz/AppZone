package jp.co.cyberagent.appzone

import javax.naming.directory.DirContext
import javax.naming.directory.InitialDirContext
import javax.naming.Context
import java.util.Hashtable
import net.liftweb.util.Props
import org.slf4j.LoggerFactory

trait AuthenticationSupport {
  val logger = LoggerFactory.getLogger(getClass)

  def loginLdap(username: String, password: String): Boolean = {
    try {
      val env: Hashtable[String, String] = new Hashtable[String, String]()
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
      env.put(Context.PROVIDER_URL, Props.get("auth.ldap.url", null))

      env.put(Context.SECURITY_AUTHENTICATION, "simple")
      env.put(Context.SECURITY_PRINCIPAL, Props.get("auth.ldap.principal", null) format (username))
      env.put(Context.SECURITY_CREDENTIALS, password)

      val ctx: DirContext = new InitialDirContext(env)
      val result = ctx != null

      if (ctx != null)
        ctx.close()

      result
    } catch {
      case e: Exception => {
        logger.info("LDAP login failure for " + username + ": " + e.getMessage)
        false
      }
    }
  }
}