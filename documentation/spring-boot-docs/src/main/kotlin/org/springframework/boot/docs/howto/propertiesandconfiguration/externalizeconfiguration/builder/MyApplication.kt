/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.propertiesandconfiguration.externalizeconfiguration.builder

import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder

object MyApplication {

	@JvmStatic
	fun main(args: Array<String>) {
		SpringApplicationBuilder()
			.bannerMode(Banner.Mode.OFF)
			.sources(MyApplication::class.java)
			.run(*args)
	}

}

