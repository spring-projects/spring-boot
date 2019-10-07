package org.springframework.boot.docs.context.properties.bind.kotlin

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.HashMap

// tag::customizer[]
@ConfigurationProperties("acme")
class MapAcmeProperties {

	val map: Map<String, MyPojo> = HashMap()

}
// end::customizer[]