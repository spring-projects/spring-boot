package org.springframework.boot.docs.configuration.kotlin

// tag::customizer[]
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

/**
 * Example showing how to access application arguments
 *
 * @author Ibanga Enoobong Ime
 */

@Component
class MyBean(args: ApplicationArguments) {

	init {
		val debug = args.containsOption("debug")
		val files = args.nonOptionArgs
		// if run with "--debug logfile.txt" debug=true, files=["logfile.txt"]
	}
}
// end::customizer[]
