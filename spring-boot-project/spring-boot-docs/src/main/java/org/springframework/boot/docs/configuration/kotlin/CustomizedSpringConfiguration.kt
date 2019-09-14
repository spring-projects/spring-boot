package org.springframework.boot.docs.configuration.kotlin

import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.runApplication

/**
 * Example showing Application Class with [SpringApplication]
 *
 * @author Ibanga Enoobong Ime
 */
// tag::customizer[]
fun main(args: Array<String>) {
	runApplication<MySpringConfiguration>(*args) {
		setBannerMode(Banner.Mode.OFF)
	}
}
// end::customizer[]
