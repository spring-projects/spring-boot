/*
 * Copyright 2012-present the original author or authors.
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
 * @see AbstractSessionReactiveAutoConfigurationTests
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
