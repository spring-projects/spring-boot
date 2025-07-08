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

package org.springframework.boot.session.hazelcast.autoconfigure;

import java.time.Duration;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.session.autoconfigure.AbstractSessionAutoConfigurationTests;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.hazelcast.HazelcastIndexedSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HazelcastSessionAutoConfiguration}.
 *
 * @author Vedran Pavic
 */
class HazelcastSessionAutoConfigurationTests extends AbstractSessionAutoConfigurationTests {

	@BeforeEach
	void prepareContextRunner() {
		this.contextRunner = new WebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(SessionAutoConfiguration.class, HazelcastSessionAutoConfiguration.class))
			.withUserConfiguration(HazelcastConfiguration.class);
	}

	@Test
	void defaultConfig() {
		this.contextRunner.run((context) -> {
			HazelcastIndexedSessionRepository repository = validateSessionRepository(context,
					HazelcastIndexedSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval",
					new ServerProperties().getServlet().getSession().getTimeout());
			HazelcastInstance hazelcastInstance = context.getBean(HazelcastInstance.class);
			then(hazelcastInstance).should().getMap("spring:session:sessions");
		});
	}

	@Test
	void defaultConfigWithCustomTimeout() {
		this.contextRunner.withPropertyValues("spring.session.timeout=1m").run((context) -> {
			HazelcastIndexedSessionRepository repository = validateSessionRepository(context,
					HazelcastIndexedSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval", Duration.ofMinutes(1));
		});
	}

	@Test
	void customMapName() {
		this.contextRunner.withPropertyValues("spring.session.hazelcast.map-name=foo:bar:biz").run((context) -> {
			validateSessionRepository(context, HazelcastIndexedSessionRepository.class);
			HazelcastInstance hazelcastInstance = context.getBean(HazelcastInstance.class);
			then(hazelcastInstance).should().getMap("foo:bar:biz");
		});
	}

	@Test
	void customFlushMode() {
		this.contextRunner.withPropertyValues("spring.session.hazelcast.flush-mode=immediate").run((context) -> {
			HazelcastIndexedSessionRepository repository = validateSessionRepository(context,
					HazelcastIndexedSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("flushMode", FlushMode.IMMEDIATE);
		});
	}

	@Test
	void customSaveMode() {
		this.contextRunner.withPropertyValues("spring.session.hazelcast.save-mode=on-get-attribute").run((context) -> {
			HazelcastIndexedSessionRepository repository = validateSessionRepository(context,
					HazelcastIndexedSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("saveMode", SaveMode.ON_GET_ATTRIBUTE);
		});
	}

	@Test
	void whenTheUserDefinesTheirOwnSessionRepositoryCustomizerThenDefaultConfigurationIsOverwritten() {
		this.contextRunner.withUserConfiguration(CustomizerConfiguration.class)
			.withPropertyValues("spring.session.hazelcast.save-mode=on-get-attribute")
			.run((context) -> {
				HazelcastIndexedSessionRepository repository = validateSessionRepository(context,
						HazelcastIndexedSessionRepository.class);
				assertThat(repository).hasFieldOrPropertyWithValue("saveMode", SaveMode.ALWAYS);
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

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfiguration {

		@Bean
		SessionRepositoryCustomizer<HazelcastIndexedSessionRepository> sessionRepositoryCustomizer() {
			return (repository) -> repository.setSaveMode(SaveMode.ALWAYS);
		}

	}

}
