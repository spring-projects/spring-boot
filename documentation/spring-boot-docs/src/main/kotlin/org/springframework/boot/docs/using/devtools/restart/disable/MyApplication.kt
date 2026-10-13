/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.using.devtools.restart.disable

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MyApplication

fun main(args: Array<String>) {
	System.setProperty("spring.devtools.restart.enabled", "false")
	runApplication<MyApplication>(*args)
}
