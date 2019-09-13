package org.springframework.boot.docs.configuration.kotlin

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Example showing Application Class with [org.springframework.boot.SpringApplication]
 *
 * @author Ibanga Enoobong Ime
 */

@SpringBootApplication
class MySpringConfiguration

// tag::customizer[]
fun main(args: Array<String>) {
	SpringApplication.run(MySpringConfiguration::class.java, *args)
}
// end::customizer[]
