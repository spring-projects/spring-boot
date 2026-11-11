/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.transaction.autoconfigure;

import java.util.Collections;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TransactionManagerCustomizationAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class TransactionManagerCustomizationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(TransactionManagerCustomizationAutoConfiguration.class));

	@Test
	void autoConfiguresTransactionManagerCustomizers() {
		this.contextRunner.withPropertyValues("spring.transaction.default-timeout=30s").run((context) -> {
			TransactionManagerCustomizers customizers = context.getBean(TransactionManagerCustomizers.class);
			assertThat(customizers).extracting("customizers")
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.hasSize(2)
				.hasAtLeastOneElementOfType(TransactionProperties.class)
				.hasAtLeastOneElementOfType(ExecutionListenersTransactionManagerCustomizer.class);
		});
	}

	@Test
	void autoConfiguredTransactionManagerCustomizersBacksOff() {
		this.contextRunner.withUserConfiguration(CustomTransactionManagerCustomizersConfiguration.class)
			.run((context) -> {
				TransactionManagerCustomizers customizers = context.getBean(TransactionManagerCustomizers.class);
				assertThat(customizers).extracting("customizers")
					.asInstanceOf(InstanceOfAssertFactories.LIST)
					.isEmpty();
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTransactionManagerCustomizersConfiguration {

		@Bean
		TransactionManagerCustomizers customTransactionManagerCustomizers() {
			return TransactionManagerCustomizers.of(Collections.<TransactionManagerCustomizer<?>>emptyList());
		}

	}

}
