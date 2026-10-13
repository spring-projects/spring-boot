/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OnWsdlLocationsCondition}.
 *
 * @author Eneias Silva
 * @author Stephane Nicoll
 */
class OnWsdlLocationsConditionTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(TestConfig.class);

	@Test
	void wsdlLocationsNotDefined() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("foo"));
	}

	@Test
	void wsdlLocationsDefinedAsCommaSeparated() {
		this.contextRunner.withPropertyValues("spring.webservices.wsdl-locations=value1")
			.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	void wsdlLocationsDefinedAsList() {
		this.contextRunner.withPropertyValues("spring.webservices.wsdl-locations[0]=value1")
			.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(OnWsdlLocationsCondition.class)
	static class TestConfig {

		@Bean
		String foo() {
			return "foo";
		}

	}

}
