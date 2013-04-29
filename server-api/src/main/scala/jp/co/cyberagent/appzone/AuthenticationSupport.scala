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
    var success = false
    var errors: List[Exception] = Nil

    def tryLoginLdap(base: String): Boolean = {
      try {
        val env: Hashtable[String, String] = new Hashtable[String, String]()

        val url: String = Props.get(base + ".url", null)
        val principal: String = Props.get(base + ".principal", null) format (username)

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
        env.put(Context.PROVIDER_URL, url)

        env.put(Context.SECURITY_AUTHENTICATION, "simple")
        env.put(Context.SECURITY_PRINCIPAL, principal)
        env.put(Context.SECURITY_CREDENTIALS, password)

        val ctx: DirContext = new InitialDirContext(env)
        val result = ctx != null

        if (ctx != null)
          ctx.close()

        result
      } catch {
        case e: Exception => {
          errors = e :: errors
          false
        }
      }
    }

    // Trying to log in
    for (i <- 0 to 5) {
      val base = if (i == 0) {
        "auth.ldap"
      } else {
        "auth.ldap." + i
      }

      if (Props.props.contains(base + ".url")) {
        success = tryLoginLdap(base)
      }
    }

    if (!success) {
      for (e <- errors) {
        logger.info("LDAP login failure for " + username + ": " + e.getMessage)
      }
    }

    success
  }
}