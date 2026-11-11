/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.registeringcustom

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.context.annotation.Bean

class MyMeterBinderConfiguration {

	@Bean
	fun queueSize(queue: Queue): MeterBinder {
		return MeterBinder { registry ->
			Gauge.builder("queueSize", queue::size).register(registry)
		}
	}

}

