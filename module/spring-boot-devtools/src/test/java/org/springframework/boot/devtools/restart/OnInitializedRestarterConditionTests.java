/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart;

import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OnInitializedRestarterCondition}.
 *
 * @author Phillip Webb
 */
class OnInitializedRestarterConditionTests {

	@BeforeEach
	@AfterEach
	void cleanup() {
		Restarter.clearInstance();
	}

	@Test
	void noInstance() {
		Restarter.clearInstance();
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		assertThat(context.containsBean("bean")).isFalse();
		context.close();
	}

	@Test
	void noInitialization() {
		Restarter.initialize(new String[0], false, RestartInitializer.NONE);
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		assertThat(context.containsBean("bean")).isFalse();
		context.close();
	}

	@Test
	void initialized() throws Exception {
		Thread thread = new Thread(TestInitialized::main);
		thread.start();
		thread.join(30000);
		assertThat(thread.isAlive()).isFalse();
	}

	static class TestInitialized {

		static void main(String... args) {
			RestartInitializer initializer = mock(RestartInitializer.class);
			given(initializer.getInitialUrls(any(Thread.class))).willReturn(new URL[0]);
			Restarter.initialize(new String[0], false, initializer);
			ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
			assertThat(context.containsBean("bean")).isTrue();
			context.close();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		@ConditionalOnInitializedRestarter
		String bean() {
			return "bean";
		}

	}

}
