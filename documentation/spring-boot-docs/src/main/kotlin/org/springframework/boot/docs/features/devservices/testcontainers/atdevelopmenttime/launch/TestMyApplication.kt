/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.devservices.testcontainers.atdevelopmenttime.launch

import org.springframework.boot.fromApplication

fun main(args: Array<String>) {
	fromApplication<MyApplication>().run(*args)
}

