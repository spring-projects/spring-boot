/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.boot.diagnostics.analyzer.BeanCurrentlyInCreationFailureAnalyzerTests.CycleWithAutowiredFields.BeanThreeConfiguration;
import org.springframework.boot.diagnostics.analyzer.BeanCurrentlyInCreationFailureAnalyzerTests.CycleWithAutowiredFields.BeanTwoConfiguration;
import org.springframework.boot.diagnostics.analyzer.BeanCurrentlyInCreationFailureAnalyzerTests.CyclicBeanMethodsConfiguration.InnerConfiguration;
import org.springframework.boot.diagnostics.analyzer.BeanCurrentlyInCreationFailureAnalyzerTests.CyclicBeanMethodsConfiguration.InnerConfiguration.InnerInnerConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link BeanCurrentlyInCreationFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class BeanCurrentlyInCreationFailureAnalyzerTests {

	private final FailureAnalyzer analyzer = new BeanCurrentlyInCreationFailureAnalyzer();

	@Test
	void cyclicBeanMethods() throws IOException {
		FailureAnalysis analysis = performAnalysis(CyclicBeanMethodsConfiguration.class);
		List<String> lines = readDescriptionLines(analysis);
		assertThat(lines).hasSize(9);
		assertThat(lines.get(0))
				.isEqualTo("The dependencies of some of the beans in the application context form a cycle:");
		assertThat(lines.get(1)).isEqualTo("");
		assertThat(lines.get(2)).isEqualTo("┌─────┐");
		assertThat(lines.get(3)).startsWith("|  one defined in " + InnerInnerConfiguration.class.getName());
		assertThat(lines.get(4)).isEqualTo("↑     ↓");
		assertThat(lines.get(5)).startsWith("|  two defined in " + InnerConfiguration.class.getName());
		assertThat(lines.get(6)).isEqualTo("↑     ↓");
		assertThat(lines.get(7)).startsWith("|  three defined in " + CyclicBeanMethodsConfiguration.class.getName());
		assertThat(lines.get(8)).isEqualTo("└─────┘");
	}

	@Test
	void cycleWithAutowiredFields() throws IOException {
		FailureAnalysis analysis = performAnalysis(CycleWithAutowiredFields.class);
		assertThat(analysis.getDescription())
				.startsWith("The dependencies of some of the beans in the application context form a cycle:");
		List<String> lines = readDescriptionLines(analysis);
		assertThat(lines).hasSize(9);
		assertThat(lines.get(0))
				.isEqualTo("The dependencies of some of the beans in the application context form a cycle:");
		assertThat(lines.get(1)).isEqualTo("");
		assertThat(lines.get(2)).isEqualTo("┌─────┐");
		assertThat(lines.get(3)).startsWith("|  three defined in " + BeanThreeConfiguration.class.getName());
		assertThat(lines.get(4)).isEqualTo("↑     ↓");
		assertThat(lines.get(5)).startsWith("|  one defined in " + CycleWithAutowiredFields.class.getName());
		assertThat(lines.get(6)).isEqualTo("↑     ↓");
		assertThat(lines.get(7)).startsWith(
				"|  " + BeanTwoConfiguration.class.getName() + " (field private " + BeanThree.class.getName());
		assertThat(lines.get(8)).isEqualTo("└─────┘");
	}

	@Test
	void cycleReferencedViaOtherBeans() throws IOException {
		FailureAnalysis analysis = performAnalysis(CycleReferencedViaOtherBeansConfiguration.class);
		List<String> lines = readDescriptionLines(analysis);
		assertThat(lines).hasSize(12);
		assertThat(lines.get(0))
				.isEqualTo("The dependencies of some of the beans in the application context form a cycle:");
		assertThat(lines.get(1)).isEqualTo("");
		assertThat(lines.get(2)).contains("refererOne (field " + RefererTwo.class.getName());
		assertThat(lines.get(3)).isEqualTo("      ↓");
		assertThat(lines.get(4)).contains("refererTwo (field " + BeanOne.class.getName());
		assertThat(lines.get(5)).isEqualTo("┌─────┐");
		assertThat(lines.get(6))
				.startsWith("|  one defined in " + CycleReferencedViaOtherBeansConfiguration.class.getName());
		assertThat(lines.get(7)).isEqualTo("↑     ↓");
		assertThat(lines.get(8))
				.startsWith("|  two defined in " + CycleReferencedViaOtherBeansConfiguration.class.getName());
		assertThat(lines.get(9)).isEqualTo("↑     ↓");
		assertThat(lines.get(10))
				.startsWith("|  three defined in " + CycleReferencedViaOtherBeansConfiguration.class.getName());
		assertThat(lines.get(11)).isEqualTo("└─────┘");
	}

	@Test
	void cycleWithAnUnknownStartIsNotAnalyzed() {
		assertThat(this.analyzer.analyze(new BeanCurrentlyInCreationException("test"))).isNull();
	}

	private List<String> readDescriptionLines(FailureAnalysis analysis) throws IOException {
		try (BufferedReader reader = new BufferedReader(new StringReader(analysis.getDescription()))) {
			return reader.lines().collect(Collectors.toList());
		}
	}

	private FailureAnalysis performAnalysis(Class<?> configuration) {
		FailureAnalysis analysis = this.analyzer.analyze(createFailure(configuration));
		assertThat(analysis).isNotNull();
		return analysis;
	}

	private Exception createFailure(Class<?> configuration) {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(configuration)) {
			fail("Expected failure did not occur");
			return null;
		}
		catch (Exception ex) {
			return ex;
		}
	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class CyclicBeanMethodsConfiguration {

		@Bean
		BeanThree three(BeanOne one) {
			return new BeanThree();
		}

		@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
		static class InnerConfiguration {

			@Bean
			BeanTwo two(BeanThree three) {
				return new BeanTwo();
			}

			@Configuration(proxyBeanMethods = false)
			static class InnerInnerConfiguration {

				@Bean
				BeanOne one(BeanTwo two) {
					return new BeanOne();
				}

			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CycleReferencedViaOtherBeansConfiguration {

		@Bean
		BeanOne one(BeanTwo two) {
			return new BeanOne();
		}

		@Bean
		BeanTwo two(BeanThree three) {
			return new BeanTwo();
		}

		@Bean
		BeanThree three(BeanOne beanOne) {
			return new BeanThree();
		}

		@Configuration(proxyBeanMethods = false)
		static class InnerConfiguration {

			@Bean
			RefererTwo refererTwo() {
				return new RefererTwo();
			}

			@Configuration(proxyBeanMethods = false)
			static class InnerInnerConfiguration {

				@Bean
				RefererOne refererOne() {
					return new RefererOne();
				}

			}

		}

	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class CycleWithAutowiredFields {

		@Bean
		BeanOne one(BeanTwo two) {
			return new BeanOne();
		}

		@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
		static class BeanTwoConfiguration {

			@SuppressWarnings("unused")
			@Autowired
			private BeanThree three;

			@Bean
			BeanTwo two() {
				return new BeanTwo();
			}

		}

		@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
		static class BeanThreeConfiguration {

			@Bean
			BeanThree three(BeanOne one) {
				return new BeanThree();
			}

		}

	}

	static class RefererOne {

		@Autowired
		RefererTwo refererTwo;

	}

	static class RefererTwo {

		@Autowired
		BeanOne beanOne;

	}

	static class BeanOne {

	}

	static class BeanTwo {

	}

	static class BeanThree {

	}

}
