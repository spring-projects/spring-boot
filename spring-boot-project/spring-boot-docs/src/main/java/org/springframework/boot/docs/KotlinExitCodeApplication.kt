package org.springframework.boot.docs

import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

/**
 * Example configuration that illustrates the use of [ExitCodeGenerator].
 *
 * @author Ibanga Enoobong Ime
 */
// tag::example[]
@SpringBootApplication
class KotlinExitCodeApplication {

	@Bean
	fun exitCodeGenerator(): ExitCodeGenerator {
		return ExitCodeGenerator { 42 }
	}

	companion object {

		@JvmStatic
		fun main(args: Array<String>) {
			System.exit(SpringApplication.exit(SpringApplication.run(KotlinExitCodeApplication::class.java, *args)))
		}
	}

}
// end::example[]
