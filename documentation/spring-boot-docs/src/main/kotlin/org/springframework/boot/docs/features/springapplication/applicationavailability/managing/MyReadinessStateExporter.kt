/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.springapplication.applicationavailability.managing

import org.springframework.boot.availability.AvailabilityChangeEvent
import org.springframework.boot.availability.ReadinessState
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class MyReadinessStateExporter {

	@EventListener
	fun onStateChange(event: AvailabilityChangeEvent<ReadinessState>) {
		when (event.state) {
			ReadinessState.ACCEPTING_TRAFFIC -> {
				// create file /tmp/healthy
			}
			ReadinessState.REFUSING_TRAFFIC -> {
				// remove file /tmp/healthy
			}
		}
	}

}

