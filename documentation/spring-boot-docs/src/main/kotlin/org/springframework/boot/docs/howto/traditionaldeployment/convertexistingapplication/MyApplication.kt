/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.traditionaldeployment.convertexistingapplication

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

@SpringBootApplication
class MyApplication : SpringBootServletInitializer() {

	override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
		// Customize the application or call application.sources(...) to add sources
		// Since our example is itself a @Configuration class (through @SpringBootApplication)
		// we actually do not need to override this method.
		return application
	}

}

// tag::main[]
fun main(args: Array<String>) {
	runApplication<MyApplication>(*args)
}
// end::main[]

