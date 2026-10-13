/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.supported.mongodb.connectionpool;

import com.mongodb.event.ConnectionPoolCreatedEvent;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.mongodb.MongoConnectionPoolTagsProvider;

public class CustomConnectionPoolTagsProvider implements MongoConnectionPoolTagsProvider {

	@Override
	public Iterable<Tag> connectionPoolTags(ConnectionPoolCreatedEvent event) {
		return java.util.Collections.emptyList();
	}

}
