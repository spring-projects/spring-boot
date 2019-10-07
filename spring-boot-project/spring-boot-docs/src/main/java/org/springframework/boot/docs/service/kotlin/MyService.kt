package org.springframework.boot.docs.service.kotlin


import org.eclipse.jetty.server.Server
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.docs.context.properties.bind.kotlin.AcmeProperties
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

/**
 * Example using [ConfigurationProperties] annotated types as bean
 *
 * @author Ibanga Enoobong Ime
 */
// tag::customizer[]
@Service
class MyService(private val properties: AcmeProperties) {

	//...

	@PostConstruct
	fun openConnection() {
		val server = Server(properties.port)
		// ...
	}
}
// end::customizer[]
