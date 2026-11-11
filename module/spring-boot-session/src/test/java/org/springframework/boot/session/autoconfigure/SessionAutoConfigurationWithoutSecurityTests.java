/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.session.autoconfigure;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.web.http.DefaultCookieSerializer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SessionAutoConfiguration} when Spring Security is not on the
 * classpath.
 *
 * @author Vedran Pavic
 */
@ClassPathExclusions("spring-security-*")
class SessionAutoConfigurationWithoutSecurityTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class));

	@Test
	void sessionCookieConfigurationIsAppliedToAutoConfiguredCookieSerializer() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class).run((context) -> {
			DefaultCookieSerializer cookieSerializer = context.getBean(DefaultCookieSerializer.class);
			assertThat(cookieSerializer).hasFieldOrPropertyWithValue("rememberMeRequestAttribute", null);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class SessionRepositoryConfiguration {

		@Bean
		MapSessionRepository mySessionRepository() {
			return new MapSessionRepository(Collections.emptyMap());
		}

	}

}
