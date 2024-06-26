/*
 * Copyright 2012-2024 the original author or authors.
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

import java.time.Duration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.actuate.data.mongo.MongoReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MongoReactiveHealthIndicator}.
 *
 * @author Andy Wilkinson
 */
@Testcontainers(disabledWithoutDocker = true)
class MongoReactiveHealthIndicatorIntegrationTests {

	@Container
	static MongoDBContainer mongo = TestImage.container(MongoDBContainer.class);

	@Test
	void standardApi() {
		Health health = mongoHealth();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
	}

	@Test
	void strictV1Api() {
		Health health = mongoHealth(ServerApi.builder().strict(true).version(ServerApiVersion.V1).build());
		assertThat(health.getStatus()).isEqualTo(Status.UP);
	}

	private Health mongoHealth() {
		return mongoHealth(null);
	}

	private Health mongoHealth(ServerApi serverApi) {
		Builder settingsBuilder = MongoClientSettings.builder()
			.applyConnectionString(new ConnectionString(mongo.getConnectionString()));
		if (serverApi != null) {
			settingsBuilder.serverApi(serverApi);
		}
		MongoClientSettings settings = settingsBuilder.build();
		MongoClient mongoClient = MongoClients.create(settings);
		MongoReactiveHealthIndicator healthIndicator = new MongoReactiveHealthIndicator(
				new ReactiveMongoTemplate(mongoClient, "db"));
		return healthIndicator.getHealth(true).block(Duration.ofSeconds(30));
	}

}
