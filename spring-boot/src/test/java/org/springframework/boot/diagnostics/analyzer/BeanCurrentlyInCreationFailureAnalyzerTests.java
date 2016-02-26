/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.diagnostics.analyzer;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link BeanCurrentlyInCreationFailureAnalyzer}
 *
 * @author Andy Wilkinson
 */
public class BeanCurrentlyInCreationFailureAnalyzerTests {

	private final FailureAnalyzer analyzer = new BeanCurrentlyInCreationFailureAnalyzer();

	@Test
	public void cyclicBeanMethods() {
		FailureAnalysis analysis = performAnalysis(CyclicBeanMethodsConfiguration.class);
		assertThat(analysis.getDescription()).startsWith(
				"There is a circular dependency between 3 beans in the application context:");
		assertThat(analysis.getDescription()).contains(
				"one defined in " + CyclicBeanMethodsConfiguration.class.getName());
		assertThat(analysis.getDescription()).contains(
				"two defined in " + CyclicBeanMethodsConfiguration.class.getName());
		assertThat(analysis.getDescription()).contains(
				"three defined in " + CyclicBeanMethodsConfiguration.class.getName());
	}

	@Test
	public void cycleWithAutowiredFields() {
		FailureAnalysis analysis = performAnalysis(CycleWithAutowiredFields.class);
		assertThat(analysis.getDescription()).startsWith(
				"There is a circular dependency between 3 beans in the application context:");
		assertThat(analysis.getDescription()).contains("three defined in "
				+ CycleWithAutowiredFields.BeanThreeConfiguration.class.getName());
		assertThat(analysis.getDescription())
				.contains("one defined in " + CycleWithAutowiredFields.class.getName());
		assertThat(analysis.getDescription())
				.contains(CycleWithAutowiredFields.BeanTwoConfiguration.class.getName()
						+ " (field private " + BeanThree.class.getName());
	}

	private FailureAnalysis performAnalysis(Class<?> configuration) {
		FailureAnalysis analysis = this.analyzer.analyze(createFailure(configuration));
		assertThat(analysis).isNotNull();
		return analysis;
	}

	private Exception createFailure(Class<?> configuration) {
		ConfigurableApplicationContext context = null;
		try {
			context = new AnnotationConfigApplicationContext(configuration);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return ex;
		}
		finally {
			if (context != null) {
				context.close();
			}
		}
		fail("Expected failure did not occur");
		return null;
	}

	@Configuration
	static class CyclicBeanMethodsConfiguration {

		@Bean
		public BeanOne one(BeanTwo two) {
			return new BeanOne();
		}

		@Bean
		public BeanTwo two(BeanThree three) {
			return new BeanTwo();
		}

		@Bean
		public BeanThree three(BeanOne one) {
			return new BeanThree();
		}

	}

	@Configuration
	public static class CycleWithAutowiredFields {

		@Bean
		public BeanOne one(BeanTwo two) {
			return new BeanOne();
		}

		public static class BeanTwoConfiguration {

			@SuppressWarnings("unused")
			@Autowired
			private BeanThree three;

			@Bean
			public BeanTwo two() {
				return new BeanTwo();
			}
		}

		public static class BeanThreeConfiguration {

			@Bean
			public BeanThree three(BeanOne one) {
				return new BeanThree();
			}
		}

	}

	static class BeanOne {

	}

	static class BeanTwo {

	}

	static class BeanThree {

	}

}
