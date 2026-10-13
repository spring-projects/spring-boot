/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.elasticsearch.connectingusingspringdata

import org.springframework.stereotype.Component

@Component
class MyBean(private val template: org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate ) {

	// @fold:on // ...
	fun someMethod(id: String): Boolean {
		return template.exists(id, User::class.java)
	}
	// @fold:off

}

