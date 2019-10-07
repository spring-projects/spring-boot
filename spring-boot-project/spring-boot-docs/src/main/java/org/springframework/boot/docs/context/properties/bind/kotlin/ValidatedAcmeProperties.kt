package org.springframework.boot.docs.context.properties.bind.kotlin

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.net.InetAddress
import javax.validation.constraints.NotNull

// tag::example[]
@ConfigurationProperties(prefix = "acme")
@Validated
class ValidatedAcmeProperties {

	@NotNull
	lateinit var remoteAddress: InetAddress

}
// end::example[]