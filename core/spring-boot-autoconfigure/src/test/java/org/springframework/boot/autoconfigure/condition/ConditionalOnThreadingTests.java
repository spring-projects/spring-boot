/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnThreading}.
 *
 * @author Moritz Halbritter
 */
class ConditionalOnThreadingTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(BasicConfiguration.class);

	@Test
	@EnabledForJreRange(max = JRE.JAVA_20)
	void platformThreadsOnJdkBelow21IfVirtualThreadsPropertyIsEnabled() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=true")
			.run((context) -> assertThat(context.getBean(ThreadType.class)).isEqualTo(ThreadType.PLATFORM));
	}

	@Test
	@EnabledForJreRange(max = JRE.JAVA_20)
	void platformThreadsOnJdkBelow21IfVirtualThreadsPropertyIsDisabled() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=false")
			.run((context) -> assertThat(context.getBean(ThreadType.class)).isEqualTo(ThreadType.PLATFORM));
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void virtualThreadsOnJdk21IfVirtualThreadsPropertyIsEnabled() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=true")
			.run((context) -> assertThat(context.getBean(ThreadType.class)).isEqualTo(ThreadType.VIRTUAL));
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void platformThreadsOnJdk21IfVirtualThreadsPropertyIsDisabled() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=false")
			.run((context) -> assertThat(context.getBean(ThreadType.class)).isEqualTo(ThreadType.PLATFORM));
	}

	private enum ThreadType {

		PLATFORM, VIRTUAL

	}

	@Configuration(proxyBeanMethods = false)
	static class BasicConfiguration {

		@Bean
		@ConditionalOnThreading(Threading.VIRTUAL)
		ThreadType virtual() {
			return ThreadType.VIRTUAL;
		}

		@Bean
		@ConditionalOnThreading(Threading.PLATFORM)
		ThreadType platform() {
			return ThreadType.PLATFORM;
		}

	}

}
