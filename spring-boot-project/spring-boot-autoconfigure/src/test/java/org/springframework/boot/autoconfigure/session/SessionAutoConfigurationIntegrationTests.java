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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link SessionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class SessionAutoConfigurationIntegrationTests
		extends AbstractSessionAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class,
					SessionAutoConfiguration.class))
			.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	public void severalCandidatesWithNoSessionStore() {
		this.contextRunner.withUserConfiguration(HazelcastConfiguration.class)
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure().hasCauseInstanceOf(
							NonUniqueSessionRepositoryException.class);
					assertThat(context).getFailure().hasMessageContaining(
							"Multiple session repository candidates are available");
					assertThat(context).getFailure().hasMessageContaining(
							"set the 'spring.session.store-type' property accordingly");
				});
	}

	@Test
	public void severalCandidatesWithWrongSessionStore() {
		this.contextRunner.withUserConfiguration(HazelcastConfiguration.class)
				.withPropertyValues("spring.session.store-type=redis").run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure().hasCauseInstanceOf(
							SessionRepositoryUnavailableException.class);
					assertThat(context).getFailure().hasMessageContaining(
							"No session repository could be auto-configured");
					assertThat(context).getFailure()
							.hasMessageContaining("session store type is 'redis'");
				});
	}

	@Test
	public void severalCandidatesWithValidSessionStore() {
		this.contextRunner.withUserConfiguration(HazelcastConfiguration.class)
				.withPropertyValues("spring.session.store-type=jdbc")
				.run((context) -> validateSessionRepository(context,
						JdbcOperationsSessionRepository.class));
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
