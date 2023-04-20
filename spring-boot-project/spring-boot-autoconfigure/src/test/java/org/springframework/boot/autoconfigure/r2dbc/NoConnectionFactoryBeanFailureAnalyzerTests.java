/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.r2dbc;

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
