package org.springframework.boot.docs.configuration.kotlin

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean

class AnotherComponent

// tag::customizer[]
@ConfigurationProperties(prefix = "another")
@Bean
fun anotherComponent(): AnotherComponent {
	// ...
	return AnotherComponent()
}
// end::customizer[]