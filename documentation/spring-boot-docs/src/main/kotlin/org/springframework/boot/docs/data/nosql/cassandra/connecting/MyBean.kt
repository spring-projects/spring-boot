/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.cassandra.connecting

import org.springframework.data.cassandra.core.CassandraTemplate
import org.springframework.stereotype.Component

@Component
class MyBean(private val template: CassandraTemplate) {

	// @fold:on // ...
	fun someMethod(): Long {
		return template.count(User::class.java)
	}
	// @fold:off

}

