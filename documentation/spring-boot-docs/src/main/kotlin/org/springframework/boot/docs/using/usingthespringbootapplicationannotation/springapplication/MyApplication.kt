/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.using.usingthespringbootapplicationannotation.springapplication

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// Same as @SpringBootConfiguration @EnableAutoConfiguration @ComponentScan
@SpringBootApplication
class MyApplication

fun main(args: Array<String>) {
	runApplication<MyApplication>(*args)
}
