package org.springframework.boot.docs.context.properties.bind.kotlin

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.convert.DurationUnit
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * A [@ConfigurationProperties][ConfigurationProperties] example that uses
 * [Duration].
 *
 * @author Ibanga Enoobong Ime
 */
// tag::example[]
@ConfigurationProperties("app.system")
class AppSystemProperties {

	@DurationUnit(ChronoUnit.SECONDS)
	var sessionTimeout = Duration.ofSeconds(30)

	var readTimeout = Duration.ofMillis(1000)

}
// end::example[]
