/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.session.autoconfigure;

import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to ensure {@link SessionAutoConfiguration} and
 * {@link SessionRepositoryFilterConfiguration} does not cause early initialization.
 *
 * @author Phillip Webb
 */
class SessionAutoConfigurationEarlyInitializationIntegrationTests {

	@Test
	void configurationIsFrozenWhenSessionRepositoryAccessed() {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
			.withSystemProperties("spring.jndi.ignore=true")
			.withPropertyValues("server.port=0")
			.withUserConfiguration(TestConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(MapSessionRepository.class));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	@ImportAutoConfiguration({ TomcatServletWebServerAutoConfiguration.class, SessionAutoConfiguration.class })
	static class TestConfiguration {

		@Bean
		MapSessionRepository mapSessionRepository(ConfigurableApplicationContext context) {
			Assert.isTrue(context.getBeanFactory().isConfigurationFrozen(), "'context' should be frozen");
			return new MapSessionRepository(new LinkedHashMap<>());
		}

	}

}
