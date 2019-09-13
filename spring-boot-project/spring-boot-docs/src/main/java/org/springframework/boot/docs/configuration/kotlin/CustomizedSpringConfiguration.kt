package org.springframework.boot.docs.configuration.kotlin

import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication

/**
 * Example showing Application Class with [SpringApplication]
 *
 * @author Ibanga Enoobong Ime
 */
// tag::customizer[]
fun main(args: Array<String>) {
	val app = SpringApplication(MySpringConfiguration::class.java)
	app.setBannerMode(Banner.Mode.OFF)
	app.run(*args)
}
// end::customizer[]
