/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryProvider;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.FilteredClassLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoConnectionFactoryBeanFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class NoConnectionFactoryBeanFailureAnalyzerTests {

	@Test
	void analyzeWhenNotNoSuchBeanDefinitionExceptionShouldReturnNull() {
		assertThat(new NoConnectionFactoryBeanFailureAnalyzer().analyze(new Exception())).isNull();
	}

	@Test
	void analyzeWhenNoSuchBeanDefinitionExceptionForDifferentTypeShouldReturnNull() {
		assertThat(
				new NoConnectionFactoryBeanFailureAnalyzer().analyze(new NoSuchBeanDefinitionException(String.class)))
			.isNull();
	}

	@Test
	void analyzeWhenNoSuchBeanDefinitionExceptionButProviderIsAvailableShouldReturnNull() {
		assertThat(new NoConnectionFactoryBeanFailureAnalyzer()
			.analyze(new NoSuchBeanDefinitionException(ConnectionFactory.class))).isNull();
	}

	@Test
	void analyzeWhenNoSuchBeanDefinitionExceptionAndNoProviderShouldAnalyze() {
		assertThat(new NoConnectionFactoryBeanFailureAnalyzer(
				new FilteredClassLoader(("META-INF/services/" + ConnectionFactoryProvider.class.getName())::equals))
			.analyze(new NoSuchBeanDefinitionException(ConnectionFactory.class))).isNotNull();
	}

}
