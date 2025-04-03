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

package org.springframework.boot.session.autoconfigure;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.web.http.SessionRepositoryFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for Spring Session auto-configuration tests when the backing store cannot be
 * reactive.
 *
 * @author Stephane Nicoll
 * @author Weix Sun
 * @see AbstractReactiveSessionAutoConfigurationTests
 */
public abstract class AbstractSessionAutoConfigurationTests {

	protected WebApplicationContextRunner contextRunner;

	@Test
	void backOffIfSessionRepositoryIsPresent() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class).run((context) -> {
			MapSessionRepository repository = validateSessionRepository(context, MapSessionRepository.class);
			assertThat(context).getBean("mySessionRepository").isSameAs(repository);
		});
	}

	protected <T extends SessionRepository<?>> T validateSessionRepository(AssertableWebApplicationContext context,
			Class<T> type) {
		assertThat(context).hasSingleBean(SessionRepositoryFilter.class);
		assertThat(context).hasSingleBean(SessionRepository.class);
		SessionRepository<?> repository = context.getBean(SessionRepository.class);
		assertThat(repository).as("Wrong session repository type").isInstanceOf(type);
		return type.cast(repository);
	}

	@Configuration
	@EnableSpringHttpSession
	static class SessionRepositoryConfiguration {

		@Bean
		MapSessionRepository mySessionRepository() {
			return new MapSessionRepository(Collections.emptyMap());
		}

	}

}
