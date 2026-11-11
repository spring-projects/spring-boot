/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.micrometertracing.gettingstarted

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@SpringBootApplication
class MyApplication {

	private val logger: Log = LogFactory.getLog(MyApplication::class.java)

	@RequestMapping("/")
	fun home(): String {
		logger.info("home() has been called")
		return "Hello, World!"
	}

}

fun main(args: Array<String>) {
	runApplication<MyApplication>(*args)
}
