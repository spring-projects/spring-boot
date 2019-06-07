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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.mongo.MongoOperationsSessionRepository;
import org.springframework.session.data.redis.RedisOperationsSessionRepository;
import org.springframework.session.hazelcast.HazelcastFlushMode;
import org.springframework.session.hazelcast.HazelcastSessionRepository;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Hazelcast specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Vedran Pavic
 */
public class SessionAutoConfigurationHazelcastTests extends AbstractSessionAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class))
			.withUserConfiguration(HazelcastConfiguration.class);

	@Test
	public void defaultConfig() {
		this.contextRunner.withPropertyValues("spring.session.store-type=hazelcast").run(this::validateDefaultConfig);
	}

	@Test
	public void defaultConfigWithUniqueStoreImplementation() {
		this.contextRunner
				.withClassLoader(new FilteredClassLoader(JdbcOperationsSessionRepository.class,
						RedisOperationsSessionRepository.class, MongoOperationsSessionRepository.class))
				.run(this::validateDefaultConfig);
	}

	private void validateDefaultConfig(AssertableWebApplicationContext context) {
		validateSessionRepository(context, HazelcastSessionRepository.class);
		HazelcastInstance hazelcastInstance = context.getBean(HazelcastInstance.class);
		verify(hazelcastInstance, times(1)).getMap("spring:session:sessions");
	}

	@Test
	public void customMapName() {
		this.contextRunner.withPropertyValues("spring.session.store-type=hazelcast",
				"spring.session.hazelcast.map-name=foo:bar:biz").run((context) -> {
					validateSessionRepository(context, HazelcastSessionRepository.class);
					HazelcastInstance hazelcastInstance = context.getBean(HazelcastInstance.class);
					verify(hazelcastInstance, times(1)).getMap("foo:bar:biz");
				});
	}

	@Test
	public void customFlushMode() {
		this.contextRunner.withPropertyValues("spring.session.store-type=hazelcast",
				"spring.session.hazelcast.flush-mode=immediate").run((context) -> {
					HazelcastSessionRepository repository = validateSessionRepository(context,
							HazelcastSessionRepository.class);
					assertThat(new DirectFieldAccessor(repository).getPropertyValue("hazelcastFlushMode"))
							.isEqualTo(HazelcastFlushMode.IMMEDIATE);
				});
	}

	@Configuration
	static class HazelcastConfiguration {

		@Bean
		@SuppressWarnings("unchecked")
		public HazelcastInstance hazelcastInstance() {
			IMap<Object, Object> map = mock(IMap.class);
			HazelcastInstance mock = mock(HazelcastInstance.class);
			given(mock.getMap("spring:session:sessions")).willReturn(map);
			given(mock.getMap("foo:bar:biz")).willReturn(map);
			return mock;
		}

	}

}
