/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.test.autoconfigure;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/**
 * Example service used with {@link DataMongoTest @DataMongoTest} tests.
 *
 * @author Michael Simons
 */
@Service
public class ExampleService {

	private final MongoTemplate mongoTemplate;

	public ExampleService(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public boolean hasCollection(String collectionName) {
		return this.mongoTemplate.collectionExists(collectionName);
	}

}
