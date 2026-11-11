/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.supported.mongodb.command

import com.mongodb.event.CommandEvent
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.mongodb.MongoCommandTagsProvider

class CustomCommandTagsProvider : MongoCommandTagsProvider {

	override fun commandTags(commandEvent: CommandEvent): Iterable<Tag> {
		return emptyList()
	}

}

