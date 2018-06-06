/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure.data.mongo;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.data.mongo.MongoFixture;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataMongoTest
@UseMongoFixtures
@MongoFixture(collection = "books", value = {
		"org/springframework/boot/test/autoconfigure/data/mongo/books.fixture" })
public class DataMongoTestFixtures {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Test
	public void testClassFixtureApplied() {
		assertThat(this.mongoTemplate.getCollection("books").find().first()
				.getObjectId("_id").equals(new ObjectId("51948e86c25f4b1d1c0d303c")))
						.isTrue();
		assertThat(this.mongoTemplate.getCollection("books").count() == 2L).isTrue();
	}

	@Test
	@MongoFixture(collection = "library", value = {
			"org/springframework/boot/test/autoconfigure/data/mongo/books.fixture" })
	public void testMethodFixtureApplied() {
		assertThat(this.mongoTemplate.getCollection("books").find().first()
				.getObjectId("_id").equals(new ObjectId("51948e86c25f4b1d1c0d303c")))
						.isTrue();
		assertThat(this.mongoTemplate.getCollection("library").find().first()
				.getObjectId("_id").equals(new ObjectId("51948e86c25f4b1d1c0d303c")))
						.isTrue();
		assertThat(this.mongoTemplate.getCollection("library").count() == 2L).isTrue();
		assertThat(this.mongoTemplate.getCollection("books").count() == 2L).isTrue();
	}
}
