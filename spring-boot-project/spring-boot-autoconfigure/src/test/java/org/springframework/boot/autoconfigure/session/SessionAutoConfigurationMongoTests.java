/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.session.data.mongo.MongoOperationsSessionRepository;
import org.springframework.session.data.redis.RedisOperationsSessionRepository;
import org.springframework.session.hazelcast.HazelcastSessionRepository;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mongo-specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class SessionAutoConfigurationMongoTests
		extends AbstractSessionAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class));

	@Test
	public void defaultConfig() {
		this.contextRunner.withPropertyValues("spring.session.store-type=mongodb")
				.withConfiguration(AutoConfigurations.of(
						EmbeddedMongoAutoConfiguration.class,
						MongoAutoConfiguration.class, MongoDataAutoConfiguration.class))
				.run(validateSpringSessionUsesMongo("sessions"));
	}

	@Test
	public void defaultConfigWithUniqueStoreImplementation() {
		this.contextRunner
				.withClassLoader(new FilteredClassLoader(HazelcastSessionRepository.class,
						JdbcOperationsSessionRepository.class,
						RedisOperationsSessionRepository.class))
				.withConfiguration(AutoConfigurations.of(
						EmbeddedMongoAutoConfiguration.class,
						MongoAutoConfiguration.class, MongoDataAutoConfiguration.class))
				.run(validateSpringSessionUsesMongo("sessions"));
	}

	@Test
	public void mongoSessionStoreWithCustomizations() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(
						EmbeddedMongoAutoConfiguration.class,
						MongoAutoConfiguration.class, MongoDataAutoConfiguration.class))
				.withPropertyValues("spring.session.store-type=mongodb",
						"spring.session.mongodb.collection-name=foo")
				.run(validateSpringSessionUsesMongo("foo"));
	}

	private ContextConsumer<AssertableWebApplicationContext> validateSpringSessionUsesMongo(
			String collectionName) {
		return (context) -> {
			MongoOperationsSessionRepository repository = validateSessionRepository(
					context, MongoOperationsSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("collectionName",
					collectionName);
		};
	}

}
