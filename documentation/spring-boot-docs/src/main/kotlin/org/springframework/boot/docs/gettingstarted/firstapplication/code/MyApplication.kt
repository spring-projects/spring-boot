/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.gettingstarted.firstapplication.code

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@SpringBootApplication
class MyApplication {

	@RequestMapping("/")
	fun home() = "Hello World!"

}

fun main(args: Array<String>) {
	runApplication<MyApplication>(*args)
}

