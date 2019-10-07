package org.springframework.boot.docs.context.properties.bind.kotlin

import org.springframework.boot.context.properties.ConfigurationProperties

// tag::customizer[]
@ConfigurationProperties(prefix = "acme.my-project.person")
class OwnerProperties {

	var firstName: String? = null

}
// end::customizer[]