/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smoketest.data.mongo;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/**
 * SampleService class.
 */
@Service
public class SampleService {

	private final MongoTemplate mongoTemplate;

	/**
     * Constructs a new SampleService with the specified MongoTemplate.
     * 
     * @param mongoTemplate the MongoTemplate to be used by the SampleService
     */
    public SampleService(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	/**
     * Checks if a collection with the given name exists in the MongoDB database.
     * 
     * @param collectionName the name of the collection to check
     * @return true if the collection exists, false otherwise
     */
    public boolean hasCollection(String collectionName) {
		return this.mongoTemplate.collectionExists(collectionName);
	}

}
