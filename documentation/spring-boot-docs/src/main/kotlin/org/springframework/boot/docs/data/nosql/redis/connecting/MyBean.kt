/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.redis.connecting

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class MyBean(private val template: StringRedisTemplate) {

	// @fold:on // ...
	fun someMethod(): Boolean {
		return template.hasKey("spring")
	}
	// @fold:off

}

