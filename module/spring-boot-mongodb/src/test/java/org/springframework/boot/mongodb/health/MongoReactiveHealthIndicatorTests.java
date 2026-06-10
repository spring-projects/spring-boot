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

import java.time.Duration;
import java.util.List;

import com.mongodb.MongoException;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link MongoReactiveHealthIndicator}.
 *
 * @author Yulin Qin
 * @author Seonwoo Jung
 */
class MongoReactiveHealthIndicatorTests {

	@Test
	void mongoIsUp() {
		MongoClient mongoClient = mock(MongoClient.class);
		given(mongoClient.listDatabaseNames()).willReturn(Flux.just("test", "admin"));
		MongoDatabase adminDatabase = mock(MongoDatabase.class);
		given(mongoClient.getDatabase("admin")).willReturn(adminDatabase);
		Document commandResult = mock(Document.class);
		given(adminDatabase.runCommand(Document.parse("{ hello: 1 }"))).willReturn(Mono.just(commandResult));
		given(commandResult.getInteger("maxWireVersion")).willReturn(10);
		MongoReactiveHealthIndicator mongoReactiveHealthIndicator = new MongoReactiveHealthIndicator(mongoClient);
		Mono<Health> health = mongoReactiveHealthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("maxWireVersion", "databases");
			assertThat(h.getDetails()).containsEntry("maxWireVersion", 10);
			assertThat(h.getDetails()).containsEntry("databases", List.of("test", "admin"));
		}).expectComplete().verify(Duration.ofSeconds(30));
		// the hello command must only be run once, never per listed database
		then(mongoClient).should(never()).getDatabase("test");
	}

	@Test
	void mongoUsesFirstDatabaseWhenAdminIsNotVisible() {
		MongoClient mongoClient = mock(MongoClient.class);
		given(mongoClient.listDatabaseNames()).willReturn(Flux.just("test"));
		MongoDatabase database = mock(MongoDatabase.class);
		given(mongoClient.getDatabase("test")).willReturn(database);
		Document commandResult = mock(Document.class);
		given(database.runCommand(Document.parse("{ hello: 1 }"))).willReturn(Mono.just(commandResult));
		given(commandResult.getInteger("maxWireVersion")).willReturn(10);
		MongoReactiveHealthIndicator mongoReactiveHealthIndicator = new MongoReactiveHealthIndicator(mongoClient);
		Mono<Health> health = mongoReactiveHealthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("maxWireVersion", "databases");
			assertThat(h.getDetails()).containsEntry("maxWireVersion", 10);
			assertThat(h.getDetails()).containsEntry("databases", List.of("test"));
		}).expectComplete().verify(Duration.ofSeconds(30));
		then(mongoClient).should(never()).getDatabase("admin");
	}

	@Test
	void mongoIsDown() {
		MongoClient mongoClient = mock(MongoClient.class);
		given(mongoClient.listDatabaseNames()).willReturn(Flux.just("admin"));
		MongoDatabase adminDatabase = mock(MongoDatabase.class);
		given(mongoClient.getDatabase("admin")).willReturn(adminDatabase);
		given(adminDatabase.runCommand(Document.parse("{ hello: 1 }")))
			.willReturn(Mono.error(new MongoException("Connection failed")));
		MongoReactiveHealthIndicator mongoReactiveHealthIndicator = new MongoReactiveHealthIndicator(mongoClient);
		Mono<Health> health = mongoReactiveHealthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.DOWN);
			assertThat(h.getDetails()).containsOnlyKeys("error");
			assertThat(h.getDetails()).containsEntry("error", MongoException.class.getName() + ": Connection failed");
		}).expectComplete().verify(Duration.ofSeconds(30));
	}

}
