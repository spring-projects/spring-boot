package org.springframework.boot.docs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Example showing Application Class with [org.springframework.boot.autoconfigure.SpringBootApplication]
 *
 * @author Ibanga Enoobong Ime
 */

// tag::customizer[]
@SpringBootApplication
class KotlinApplication

fun main(args: Array<String>) {
	runApplication<KotlinApplication>(*args)
}

// end::customizer[]
