package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.conversion.durations.javabeanbinding

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.convert.DurationUnit
import java.time.Duration
import java.time.temporal.ChronoUnit

@ConfigurationProperties("my")
class MyProperties {
	@DurationUnit(ChronoUnit.SECONDS)
	var sessionTimeout = Duration.ofSeconds(30)

	var readTimeout = Duration.ofMillis(1000)
}