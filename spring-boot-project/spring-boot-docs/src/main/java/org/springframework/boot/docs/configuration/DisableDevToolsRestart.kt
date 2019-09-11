package org.springframework.boot.docs.configuration

import org.springframework.boot.docs.Application
import org.springframework.boot.runApplication

/**
 * Sample showing how to disable dev tools restart
 */
// tag::customizer[]
fun main(args: Array<String>) {
	System.setProperty("spring.devtools.restart.enabled", "false")
	runApplication<Application>(*args)
}
// end::customizer[]
