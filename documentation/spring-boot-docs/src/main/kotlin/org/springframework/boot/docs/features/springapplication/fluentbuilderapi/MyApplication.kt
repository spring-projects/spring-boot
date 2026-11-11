/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.springapplication.fluentbuilderapi

import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder

class MyApplication {
	fun hierarchyWithDisabledBanner(args: Array<String>) {
		// tag::code[]
		SpringApplicationBuilder()
			.sources(Parent::class.java)
			.child(Application::class.java)
			.bannerMode(Banner.Mode.OFF)
			.run(*args)
		// end::code[]
	}

	internal class Parent

	internal class Application

}

