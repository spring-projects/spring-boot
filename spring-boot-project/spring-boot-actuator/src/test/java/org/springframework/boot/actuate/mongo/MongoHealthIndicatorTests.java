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

package org.springframework.boot.actuate.mongo;

import com.mongodb.MongoException;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.data.mongo.MongoHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MongoHealthIndicator}.
 *
 * @author Christian Dupuis
 */
class MongoHealthIndicatorTests {

	@Test
	void mongoIsUp() {
		Document commandResult = mock(Document.class);
		given(commandResult.getInteger("maxWireVersion")).willReturn(10);
		MongoTemplate mongoTemplate = mock(MongoTemplate.class);
		given(mongoTemplate.executeCommand("{ isMaster: 1 }")).willReturn(commandResult);
		MongoHealthIndicator healthIndicator = new MongoHealthIndicator(mongoTemplate);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("maxWireVersion", 10);
		then(commandResult).should().getInteger("maxWireVersion");
		then(mongoTemplate).should().executeCommand("{ isMaster: 1 }");
	}

	@Test
	void mongoIsDown() {
		MongoTemplate mongoTemplate = mock(MongoTemplate.class);
		given(mongoTemplate.executeCommand("{ isMaster: 1 }")).willThrow(new MongoException("Connection failed"));
		MongoHealthIndicator healthIndicator = new MongoHealthIndicator(mongoTemplate);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat((String) health.getDetails().get("error")).contains("Connection failed");
		then(mongoTemplate).should().executeCommand("{ isMaster: 1 }");
	}

}
