/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.supported.mongodb.connectionpool

import com.mongodb.event.ConnectionPoolCreatedEvent
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.mongodb.MongoConnectionPoolTagsProvider

class CustomConnectionPoolTagsProvider : MongoConnectionPoolTagsProvider {

	override fun connectionPoolTags(event: ConnectionPoolCreatedEvent): Iterable<Tag> {
		return emptyList()
	}

}

