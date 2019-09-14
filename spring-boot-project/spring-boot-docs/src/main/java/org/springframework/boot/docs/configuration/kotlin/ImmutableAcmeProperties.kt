package org.springframework.boot.docs.configuration.kotlin

/**
 * Example configuration that illustrates the use of external properties in immutable fashion
 *
 * @author Ibanga Enoobong Ime
 */
// tag::customizer[]
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import java.net.InetAddress

@ConfigurationProperties("acme")
class ImmutableAcmeProperties(
	val enabled: Boolean,
	val remoteAddress: InetAddress,
	val port: Int,
	val security: Security
) {

	class Security(
		val username: String,
		val password: String,
		@DefaultValue("USER")
		val roles: List<String>
	)
}
// end::customizer[]
