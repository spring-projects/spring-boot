/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.propertiesandconfiguration.externalizeconfiguration.application

import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
object MyApplication {

	@JvmStatic
	fun main(args: Array<String>) {
		val application = SpringApplication(MyApplication::class.java)
		application.setBannerMode(Banner.Mode.OFF)
		application.run(*args)
	}

}

