/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kotlinsample

import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
open class TestKotlinApplication

fun main(args: Array<String>) {
	runApplication<TestKotlinApplication>(*args)
}

