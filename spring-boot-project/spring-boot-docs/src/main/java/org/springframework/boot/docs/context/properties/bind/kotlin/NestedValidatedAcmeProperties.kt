package org.springframework.boot.docs.context.properties.bind.kotlin

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.net.InetAddress
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

// tag::example[]
@ConfigurationProperties(prefix = "acme")
@Validated
class NestedValidatedAcmeProperties {

	@NotNull
	lateinit var remoteAddress: InetAddress

	@Valid
	val security = Security()

	class Security {

		@NotEmpty
		lateinit var username: String


	}
}
// end::example[]