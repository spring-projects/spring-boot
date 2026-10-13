/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.endpoints.health.writingcustomhealthindicators

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component
class MyHealthIndicator : HealthIndicator {

	override fun health(): Health {
		val errorCode = check()
		if (errorCode != 0) {
			return Health.down().withDetail("Error Code", errorCode).build()
		}
		return Health.up().build()
	}

	private fun check(): Int {
		// perform some specific health check
		return  /**/ 0
	}

}

