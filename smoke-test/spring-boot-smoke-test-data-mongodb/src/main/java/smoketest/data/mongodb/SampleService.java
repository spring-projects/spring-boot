/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.mongodb;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class SampleService {

	private final MongoTemplate mongoTemplate;

	public SampleService(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public boolean hasCollection(String collectionName) {
		return this.mongoTemplate.collectionExists(collectionName);
	}

}
