package org.springframework.boot.docs.configuration.kotlin

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.ArrayList

// tag::customizer[]
@ConfigurationProperties(prefix = "my")
class Config {

	val servers: List<String> = ArrayList()
}
// end::customizer[]