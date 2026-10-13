/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.mongodb.connecting

import com.mongodb.client.MongoCollection
import org.bson.Document
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.stereotype.Component

@Component
class MyBean(private val mongo: MongoDatabaseFactory) {

	// @fold:on // ...
	fun someMethod(): MongoCollection<Document> {
		val db = mongo.mongoDatabase
		return db.getCollection("users")
	}
	// @fold:off

}

