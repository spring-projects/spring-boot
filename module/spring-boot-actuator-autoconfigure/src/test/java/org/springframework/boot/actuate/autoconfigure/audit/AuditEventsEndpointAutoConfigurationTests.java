/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.audit;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.audit.AuditEventsEndpoint;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AuditEventsEndpointAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Vedran Pavic
 */
class AuditEventsEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(AuditAutoConfiguration.class, AuditEventsEndpointAutoConfiguration.class));

	@Test
	void runWhenRepositoryBeanAvailableShouldHaveEndpointBean() {
		this.contextRunner.withUserConfiguration(CustomAuditEventRepositoryConfiguration.class)
			.withPropertyValues("management.endpoints.web.exposure.include=auditevents")
			.run((context) -> assertThat(context).hasSingleBean(AuditEventsEndpoint.class));
	}

	@Test
	void endpointBacksOffWhenRepositoryNotAvailable() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=auditevents")
			.run((context) -> assertThat(context).doesNotHaveBean(AuditEventsEndpoint.class));
	}

	@Test
	void runWhenNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(AuditEventsEndpoint.class));
	}

	@Test
	void runWhenEnabledPropertyIsFalseShouldNotHaveEndpoint() {
		this.contextRunner.withUserConfiguration(CustomAuditEventRepositoryConfiguration.class)
			.withPropertyValues("management.endpoint.auditevents.enabled:false")
			.withPropertyValues("management.endpoints.web.exposure.include=*")
			.run((context) -> assertThat(context).doesNotHaveBean(AuditEventsEndpoint.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomAuditEventRepositoryConfiguration {

		@Bean
		InMemoryAuditEventRepository testAuditEventRepository() {
			return new InMemoryAuditEventRepository();
		}

	}

}
