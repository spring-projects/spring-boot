/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.Collection;

import org.junit.After;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.session.SessionRepository;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Share test utilities for {@link SessionAutoConfiguration} tests.
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractSessionAutoConfigurationTests {

	protected AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	protected <T extends SessionRepository<?>> T validateSessionRepository(
			Class<T> type) {
		SessionRepository<?> repository = this.context.getBean(SessionRepository.class);
		assertThat(repository).as("Wrong session repository type").isInstanceOf(type);
		return type.cast(repository);
	}

	protected Integer getSessionTimeout(SessionRepository<?> sessionRepository) {
		return (Integer) new DirectFieldAccessor(sessionRepository)
				.getPropertyValue("defaultMaxInactiveInterval");
	}

	protected void load(String... environment) {
		load(null, environment);
	}

	protected void load(Collection<Class<?>> configs, String... environment) {
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(ctx, environment);
		if (configs != null) {
			ctx.register(configs.toArray(new Class<?>[configs.size()]));
		}
		ctx.register(ServerPropertiesAutoConfiguration.class,
				SessionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

}
