package org.springframework.boot.docs.configuration.kotlin

// tag::customizer[]
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Sample showing how to use externalized properties with the [Value] annotation
 *
 * @author Ibanga Enoobong Ime
 */

@Component
class UsingExternalizedProperties {

	@Value("\${name}")
	private lateinit var name: String

	// ...

}
// end::customizer[]
