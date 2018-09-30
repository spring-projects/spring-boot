/*
 * Copyright 2012-2017 the original author or authors.
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

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.server.session.WebSessionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared test utilities for {@link SessionAutoConfiguration} tests.
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractSessionAutoConfigurationTests {

	protected <T extends SessionRepository<?>> T validateSessionRepository(
			AssertableWebApplicationContext context, Class<T> type) {
		assertThat(context).hasSingleBean(SessionRepositoryFilter.class);
		assertThat(context).hasSingleBean(SessionRepository.class);
		SessionRepository<?> repository = context.getBean(SessionRepository.class);
		assertThat(repository).as("Wrong session repository type").isInstanceOf(type);
		return type.cast(repository);
	}

	protected Integer getSessionTimeout(SessionRepository<?> sessionRepository) {
		return (Integer) new DirectFieldAccessor(sessionRepository)
				.getPropertyValue("defaultMaxInactiveInterval");
	}

	protected <T extends ReactiveSessionRepository<?>> T validateSessionRepository(
			AssertableReactiveWebApplicationContext context, Class<T> type) {
		assertThat(context).hasSingleBean(WebSessionManager.class);
		assertThat(context).hasSingleBean(ReactiveSessionRepository.class);
		ReactiveSessionRepository<?> repository = context
				.getBean(ReactiveSessionRepository.class);
		assertThat(repository).as("Wrong session repository type").isInstanceOf(type);
		return type.cast(repository);
	}

	protected Integer getSessionTimeout(ReactiveSessionRepository<?> sessionRepository) {
		return (Integer) new DirectFieldAccessor(sessionRepository)
				.getPropertyValue("defaultMaxInactiveInterval");
	}

}
