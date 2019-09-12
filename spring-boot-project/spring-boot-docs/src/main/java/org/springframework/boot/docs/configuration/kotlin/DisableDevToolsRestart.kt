package org.springframework.boot.docs.configuration.kotlin

import org.springframework.boot.docs.KotlinApplication
import org.springframework.boot.runApplication

/**
 * Sample showing how to disable dev tools restart
 *
 * @author Ibanga Enoobong Ime
 */
// tag::customizer[]
fun main(args: Array<String>) {
	System.setProperty("spring.devtools.restart.enabled", "false")
	runApplication<KotlinApplication>(*args)
}
// end::customizer[]
