package org.springframework.boot.docs.configuration.kotlin

/**
 * Example configuration that illustrates the use of external properties.
 *
 * @author Ibanga Enoobong Ime
 */
// tag::customizer[]
import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.InetAddress

@ConfigurationProperties("acme")
class AcmeProperties {

	private var enabled: Boolean = false

	private lateinit var remoteAddress: InetAddress

	private val security = Security()

	class Security {
		private lateinit var username: String

		private lateinit var password: String

		private var roles = arrayListOf("USER")
	}
}
// end::customizer[]
