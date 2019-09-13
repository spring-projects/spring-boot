package org.springframework.boot.docs.builder.kotlin

import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder

/**
 * Examples of using [SpringApplicationBuilder].
 *
 * @author Ibanga Enoobong Ime
 */
class SpringApplicationBuilderExample {

	fun hierarchyWithDisabledBanner(args: Array<String>) {
		// @formatter:off
		// tag::hierarchy[]
		SpringApplicationBuilder()
			.sources(Parent::class.java)
			.child(Application::class.java)
			.bannerMode(Banner.Mode.OFF)
			.run(*args)
		// end::hierarchy[]
		// @formatter:on
	}

	/**
	 * Parent application configuration.
	 */
	internal class Parent

	/**
	 * Application configuration.
	 */
	internal class Application

}
