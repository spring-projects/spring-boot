package org.springframework.boot.docs.context.properties.bind.kotlin

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.ArrayList

class MyPojo

// tag::customizer[]
@ConfigurationProperties("acme")
class ComplexAcmeProperties {

	val list: List<MyPojo> = ArrayList()
}
// end::customizer[]