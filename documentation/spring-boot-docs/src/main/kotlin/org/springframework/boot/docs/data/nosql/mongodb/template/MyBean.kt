/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.mongodb.template

import com.mongodb.client.MongoCollection
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component

@Component
class MyBean(private val mongoTemplate: MongoTemplate) {

	// @fold:on // ...
	fun someMethod(): MongoCollection<Document> {
		return mongoTemplate.getCollection("users")
	}
	// @fold:off

}

