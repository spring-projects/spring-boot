/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.traditionaldeployment.convertexistingapplication.both

import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

@SpringBootApplication
class MyApplication : SpringBootServletInitializer() {

	override fun configure(builder: SpringApplicationBuilder): SpringApplicationBuilder {
		return customizerBuilder(builder)
	}

	companion object {

		@JvmStatic
		fun main(args: Array<String>) {
			customizerBuilder(SpringApplicationBuilder()).run(*args)
		}

		private fun customizerBuilder(builder: SpringApplicationBuilder): SpringApplicationBuilder {
			return builder.sources(MyApplication::class.java).bannerMode(Banner.Mode.OFF)
		}

	}

}

