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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;
import org.springframework.session.data.mongo.MongoIndexedSessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.hazelcast.Hazelcast4IndexedSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Hazelcast specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Vedran Pavic
 */
class SessionAutoConfigurationHazelcastTests extends AbstractSessionAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class))
			.withUserConfiguration(HazelcastConfiguration.class);

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

	@Test
	void defaultConfigWithCustomTimeout() {
		this.contextRunner.withPropertyValues("spring.session.store-type=hazelcast", "spring.session.timeout=1m")
				.run((context) -> {
					Hazelcast4IndexedSessionRepository repository = validateSessionRepository(context,
							Hazelcast4IndexedSessionRepository.class);
					assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval", 60);
				});
	}

	private void validateDefaultConfig(AssertableWebApplicationContext context) {
		Hazelcast4IndexedSessionRepository repository = validateSessionRepository(context,
				Hazelcast4IndexedSessionRepository.class);
		assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval",
				(int) new ServerProperties().getServlet().getSession().getTimeout().getSeconds());
		HazelcastInstance hazelcastInstance = context.getBean(HazelcastInstance.class);
		then(hazelcastInstance).should().getMap("spring:session:sessions");
	}

	@Test
	void customMapName() {
		this.contextRunner.withPropertyValues("spring.session.store-type=hazelcast",
				"spring.session.hazelcast.map-name=foo:bar:biz").run((context) -> {
					validateSessionRepository(context, Hazelcast4IndexedSessionRepository.class);
					HazelcastInstance hazelcastInstance = context.getBean(HazelcastInstance.class);
					then(hazelcastInstance).should().getMap("foo:bar:biz");
				});
	}

	@Test
	void customFlushMode() {
		this.contextRunner.withPropertyValues("spring.session.store-type=hazelcast",
				"spring.session.hazelcast.flush-mode=immediate").run((context) -> {
					Hazelcast4IndexedSessionRepository repository = validateSessionRepository(context,
							Hazelcast4IndexedSessionRepository.class);
					assertThat(repository).hasFieldOrPropertyWithValue("flushMode", FlushMode.IMMEDIATE);
				});
	}

	@Test
	void customSaveMode() {
		this.contextRunner.withPropertyValues("spring.session.store-type=hazelcast",
				"spring.session.hazelcast.save-mode=on-get-attribute").run((context) -> {
					Hazelcast4IndexedSessionRepository repository = validateSessionRepository(context,
							Hazelcast4IndexedSessionRepository.class);
					assertThat(repository).hasFieldOrPropertyWithValue("saveMode", SaveMode.ON_GET_ATTRIBUTE);
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class HazelcastConfiguration {

		@Bean
		@SuppressWarnings("unchecked")
		HazelcastInstance hazelcastInstance() {
			IMap<Object, Object> map = mock(IMap.class);
			HazelcastInstance mock = mock(HazelcastInstance.class);
			given(mock.getMap("spring:session:sessions")).willReturn(map);
			given(mock.getMap("foo:bar:biz")).willReturn(map);
			return mock;
		}

	}

}
