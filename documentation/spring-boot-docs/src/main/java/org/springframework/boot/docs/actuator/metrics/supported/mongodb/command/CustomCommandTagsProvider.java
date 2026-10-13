/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.supported.mongodb.command;

import com.mongodb.event.CommandEvent;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.mongodb.MongoCommandTagsProvider;

class CustomCommandTagsProvider implements MongoCommandTagsProvider {

	@Override
	public Iterable<Tag> commandTags(CommandEvent commandEvent) {
		return java.util.Collections.emptyList();
	}

}
