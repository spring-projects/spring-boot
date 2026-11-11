/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.couchbase.repositories

import org.springframework.data.couchbase.core.CouchbaseTemplate
import org.springframework.stereotype.Component

@Component
class MyBean(private val template: CouchbaseTemplate) {

	// @fold:on // ...
	fun someMethod(): String {
		return template.bucketName
	}
	// @fold:off

}

