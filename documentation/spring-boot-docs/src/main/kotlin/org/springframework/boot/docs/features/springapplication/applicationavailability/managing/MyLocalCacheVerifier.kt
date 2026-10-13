/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.springapplication.applicationavailability.managing

import org.springframework.boot.availability.AvailabilityChangeEvent
import org.springframework.boot.availability.LivenessState
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class MyLocalCacheVerifier(private val eventPublisher: ApplicationEventPublisher) {

	fun checkLocalCache() {
		try {
			// ...
		} catch (ex: CacheCompletelyBrokenException) {
			AvailabilityChangeEvent.publish(eventPublisher, ex, LivenessState.BROKEN)
		}
	}

}

