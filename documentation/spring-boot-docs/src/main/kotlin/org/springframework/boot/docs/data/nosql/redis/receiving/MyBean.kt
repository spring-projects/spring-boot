/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.redis.receiving

import org.springframework.data.redis.annotation.RedisListener
import org.springframework.stereotype.Component

@Component
class MyBean {

	@RedisListener("someChannel")
	fun processMessage(content: String) {
		// ...
	}

}
