/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.mongodb.health;

import java.util.List;
import java.util.function.Consumer;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MongoHealthIndicator}.
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
 */
class MongoHealthIndicatorTests {

	@Test
	@SuppressWarnings("unchecked")
	void mongoIsUp() {
		Document commandResult = mock(Document.class);
		given(commandResult.getInteger("maxWireVersion")).willReturn(10);
		MongoClient mongoClient = mock(MongoClient.class);
		MongoIterable<String> databaseNames = mock(MongoIterable.class);
		willAnswer((invocation) -> {
			((Consumer<String>) invocation.getArgument(0)).accept("db");
			return null;
		}).given(databaseNames).forEach(any());
		given(mongoClient.listDatabaseNames()).willReturn(databaseNames);
		MongoDatabase mongoDatabase = mock(MongoDatabase.class);
		given(mongoClient.getDatabase("db")).willReturn(mongoDatabase);
		given(mongoDatabase.runCommand(Document.parse("{ hello: 1 }"))).willReturn(commandResult);
		MongoHealthIndicator healthIndicator = new MongoHealthIndicator(mongoClient);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("maxWireVersion", 10);
		assertThat(health.getDetails()).containsEntry("databases", List.of("db"));
		then(commandResult).should().getInteger("maxWireVersion");
	}

	@Test
	void mongoIsDown() {
		MongoClient mongoClient = mock(MongoClient.class);
		given(mongoClient.listDatabaseNames()).willThrow(new MongoException("Connection failed"));
		MongoHealthIndicator healthIndicator = new MongoHealthIndicator(mongoClient);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat((String) health.getDetails().get("error")).contains("Connection failed");
	}

}
