/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;
import org.springframework.session.data.mongo.MongoIndexedSessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.hazelcast.HazelcastIndexedSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hazelcast 3 specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Vedran Pavic
 */
@ClassPathExclusions("hazelcast*.jar")
@ClassPathOverrides("com.hazelcast:hazelcast:3.12.8")
class SessionAutoConfigurationHazelcast3Tests extends AbstractSessionAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HazelcastAutoConfiguration.class, SessionAutoConfiguration.class))
			.withPropertyValues(
					"spring.hazelcast.config=org/springframework/boot/autoconfigure/session/hazelcast-3.xml");

	@Test
	void defaultConfig() {
		this.contextRunner.withPropertyValues("spring.session.store-type=hazelcast").run(this::validateDefaultConfig);
	}

	@Test
	void defaultConfigWithUniqueStoreImplementation() {
		this.contextRunner
				.withClassLoader(new FilteredClassLoader(JdbcIndexedSessionRepository.class,
						RedisIndexedSessionRepository.class, MongoIndexedSessionRepository.class))
				.run(this::validateDefaultConfig);
	}

	private void validateDefaultConfig(AssertableWebApplicationContext context) {
		validateSessionRepository(context, HazelcastIndexedSessionRepository.class);
	}

	@Test
	void customMapName() {
		this.contextRunner
				.withPropertyValues("spring.session.store-type=hazelcast",
						"spring.session.hazelcast.map-name=foo:bar:biz")
				.run((context) -> validateSessionRepository(context, HazelcastIndexedSessionRepository.class));
	}

	@Test
	void customFlushMode() {
		this.contextRunner.withPropertyValues("spring.session.store-type=hazelcast",
				"spring.session.hazelcast.flush-mode=immediate").run((context) -> {
					HazelcastIndexedSessionRepository repository = validateSessionRepository(context,
							HazelcastIndexedSessionRepository.class);
					assertThat(repository).hasFieldOrPropertyWithValue("flushMode", FlushMode.IMMEDIATE);
				});
	}

	@Test
	void customSaveMode() {
		this.contextRunner.withPropertyValues("spring.session.store-type=hazelcast",
				"spring.session.hazelcast.save-mode=on-get-attribute").run((context) -> {
					HazelcastIndexedSessionRepository repository = validateSessionRepository(context,
							HazelcastIndexedSessionRepository.class);
					assertThat(repository).hasFieldOrPropertyWithValue("saveMode", SaveMode.ON_GET_ATTRIBUTE);
				});
	}

}
