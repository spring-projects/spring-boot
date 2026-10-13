/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.springapplication.applicationarguments

import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

@Component
class MyBean(args: ApplicationArguments) {

	init {
		val debug = args.containsOption("debug")
		val files = args.nonOptionArgs
		if (debug) {
			println(files)
		}
		// if run with "--debug logfile.txt" prints ["logfile.txt"]
	}

}

