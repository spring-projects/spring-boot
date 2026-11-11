/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.registeringcustom

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.springframework.stereotype.Component

@Component
class MyBean(registry: MeterRegistry) {

	private val dictionary: Dictionary

	init {
		dictionary = Dictionary.load()
		registry.gauge("dictionary.size", Tags.empty(), dictionary.words.size)
	}

}

