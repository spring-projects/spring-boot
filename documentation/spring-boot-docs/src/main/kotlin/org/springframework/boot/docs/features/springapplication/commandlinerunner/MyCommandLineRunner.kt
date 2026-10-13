/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.springapplication.commandlinerunner

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class MyCommandLineRunner : CommandLineRunner {

	override fun run(vararg args: String) {
		// Do something...
	}

}

