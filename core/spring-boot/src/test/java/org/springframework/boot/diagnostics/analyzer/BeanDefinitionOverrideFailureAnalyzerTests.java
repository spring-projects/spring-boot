/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.diagnostics.analyzer;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanDefinitionOverrideFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class BeanDefinitionOverrideFailureAnalyzerTests {

	@Test
	void analyzeBeanDefinitionOverrideException() {
		FailureAnalysis analysis = performAnalysis(BeanOverrideConfiguration.class);
		assertThat(analysis).isNotNull();
		String description = analysis.getDescription();
		assertThat(description).contains("The bean 'testBean', defined in " + SecondConfiguration.class.getName()
				+ ", could not be registered.");
		assertThat(description).contains(FirstConfiguration.class.getName());
	}

	@Test
	void analyzeBeanDefinitionOverrideExceptionWithDefinitionsWithNoResourceDescription() {
		FailureAnalysis analysis = performAnalysis((context) -> {
			context.registerBean("testBean", String.class, (Supplier<String>) String::new);
			context.registerBean("testBean", String.class, (Supplier<String>) String::new);
		});
		assertThat(analysis).isNotNull();
		String description = analysis.getDescription();
		assertThat(description)
			.isEqualTo("The bean 'testBean' could not be registered. A bean with that name has already"
					+ " been defined and overriding is disabled.");
	}

	private @Nullable FailureAnalysis performAnalysis(Class<?> configuration) {
		BeanDefinitionOverrideException failure = createFailure(configuration);
		assertThat(failure).isNotNull();
		return new BeanDefinitionOverrideFailureAnalyzer().analyze(failure);
	}

	private @Nullable BeanDefinitionOverrideException createFailure(Class<?> configuration) {
		try {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.setAllowBeanDefinitionOverriding(false);
			context.register(configuration);
			context.refresh();
			context.close();
			return null;
		}
		catch (BeanDefinitionOverrideException ex) {
			return ex;
		}
	}

	private @Nullable FailureAnalysis performAnalysis(
			ApplicationContextInitializer<AnnotationConfigApplicationContext> initializer) {
		BeanDefinitionOverrideException failure = createFailure(initializer);
		assertThat(failure).isNotNull();
		return new BeanDefinitionOverrideFailureAnalyzer().analyze(failure);
	}

	private @Nullable BeanDefinitionOverrideException createFailure(
			ApplicationContextInitializer<AnnotationConfigApplicationContext> initializer) {
		try {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.setAllowBeanDefinitionOverriding(false);
			initializer.initialize(context);
			context.refresh();
			context.close();
			return null;
		}
		catch (BeanDefinitionOverrideException ex) {
			return ex;
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ FirstConfiguration.class, SecondConfiguration.class })
	static class BeanOverrideConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class FirstConfiguration {

		@Bean
		String testBean() {
			return "test";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SecondConfiguration {

		@Bean
		String testBean() {
			return "test";
		}

	}

}
