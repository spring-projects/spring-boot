/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.netty.autoconfigure;

import io.netty.util.ResourceLeakDetector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NettyAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class NettyAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(NettyAutoConfiguration.class));

	@AfterEach
	void resetResourceLeakDetector() {
		ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
	}

	@Test
	void leakDetectionShouldBeConfigured() {
		this.contextRunner.withPropertyValues("spring.netty.leak-detection=paranoid")
			.run((context) -> assertThat(ResourceLeakDetector.getLevel())
				.isEqualTo(ResourceLeakDetector.Level.PARANOID));
	}

	@Test
	void leakDetectionShouldBeConfiguredWhenLazyInitializationIsEnabled() {
		this.contextRunner
			.withInitializer(
					(context) -> context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor()))
			.withPropertyValues("spring.netty.leak-detection=advanced")
			.run((context) -> assertThat(ResourceLeakDetector.getLevel())
				.isEqualTo(ResourceLeakDetector.Level.ADVANCED));
	}

}
