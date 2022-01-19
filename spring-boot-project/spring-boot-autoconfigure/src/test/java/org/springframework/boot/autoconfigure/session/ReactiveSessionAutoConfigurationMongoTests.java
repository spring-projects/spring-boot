/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.session.data.mongo.ReactiveMongoSessionRepository;
import org.springframework.session.data.redis.ReactiveRedisSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mongo-specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
@Testcontainers(disabledWithoutDocker = true)
class ReactiveSessionAutoConfigurationMongoTests extends AbstractSessionAutoConfigurationTests {

	@Container
	static final MongoDBContainer mongoDb = new MongoDBContainer(DockerImageNames.mongo()).withStartupAttempts(5)
			.withStartupTimeout(Duration.ofMinutes(5));

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class));

	@Test
	void defaultConfig() {
		this.contextRunner
				.withPropertyValues("spring.session.store-type=mongodb",
						"spring.data.mongodb.uri=" + mongoDb.getReplicaSetUrl())
				.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
						MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class))
				.run(validateSpringSessionUsesMongo("sessions"));
	}

	@Test
	void defaultConfigWithUniqueStoreImplementation() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ReactiveRedisSessionRepository.class))
				.withPropertyValues("spring.data.mongodb.uri=" + mongoDb.getReplicaSetUrl())
				.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
						MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class))
				.run(validateSpringSessionUsesMongo("sessions"));
	}

	@Test
	void defaultConfigWithCustomTimeout() {
		this.contextRunner
				.withPropertyValues("spring.session.store-type=mongodb", "spring.session.timeout=1m",
						"spring.data.mongodb.uri=" + mongoDb.getReplicaSetUrl())
				.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
						MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class))
				.run((context) -> {
					ReactiveMongoSessionRepository repository = validateSessionRepository(context,
							ReactiveMongoSessionRepository.class);
					assertThat(repository).hasFieldOrPropertyWithValue("maxInactiveIntervalInSeconds", 60);
				});
	}

	@Test
	void mongoSessionStoreWithCustomizations() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
						MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class))
				.withPropertyValues("spring.session.store-type=mongodb", "spring.session.mongodb.collection-name=foo",
						"spring.data.mongodb.uri=" + mongoDb.getReplicaSetUrl())
				.run(validateSpringSessionUsesMongo("foo"));
	}

	private ContextConsumer<AssertableReactiveWebApplicationContext> validateSpringSessionUsesMongo(
			String collectionName) {
		return (context) -> {
			ReactiveMongoSessionRepository repository = validateSessionRepository(context,
					ReactiveMongoSessionRepository.class);
			assertThat(repository.getCollectionName()).isEqualTo(collectionName);
			assertThat(repository).hasFieldOrPropertyWithValue("maxInactiveIntervalInSeconds",
					ReactiveMongoSessionRepository.DEFAULT_INACTIVE_INTERVAL);
		};
	}

}
