package org.springframework.boot.docs.configuration.kotlin

/**
 * Example configuration that illustrates the use of external properties.
 *
 * @author Ibanga Enoobong Ime
 */
// tag::customizer[]
import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.InetSocketAddress

@ConfigurationProperties("acme")
class AcmeProperties {

	var enabled: Boolean = false

	lateinit var remoteAddress: InetSocketAddress

	var port: Int = 0

	val security = Security()

	class Security {
		lateinit var username: String

		lateinit var password: String

		var roles = arrayListOf("USER")
	}
}
// end::customizer[]
