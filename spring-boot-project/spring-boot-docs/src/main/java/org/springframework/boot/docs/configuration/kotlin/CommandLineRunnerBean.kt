package org.springframework.boot.docs.configuration.kotlin

// tag::customizer[]
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * Example showing use of [CommandLineRunner]
 *
 * @author Ibanga Enoobong Ime
 */

@Component
class CommandLineRunnerBean : CommandLineRunner {

	override fun run(vararg args: String) {
		// Do something...
	}
}
// end::customizer[]
