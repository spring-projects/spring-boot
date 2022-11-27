package org.springframework.boot.test.context

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.runApplication

@SpringBootConfiguration(proxyBeanMethods = false)
open class KotlinApplicationWithMainThrowingException {
}

fun main(args: Array<String>) {
	runApplication<KotlinApplicationWithMainThrowingException>(*args)
	throw IllegalStateException("ThrownFromMain")
}
