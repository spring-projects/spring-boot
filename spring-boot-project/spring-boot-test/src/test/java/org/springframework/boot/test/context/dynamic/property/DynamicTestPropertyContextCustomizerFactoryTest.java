/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.context.dynamic.property;

import java.util.Arrays;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link DynamicTestPropertyContextCustomizerFactory}
 *
 * @author Anatoliy Korovin
 */
class DynamicTestPropertyContextCustomizerFactoryTest {

	private DynamicTestPropertyContextCustomizerFactory customizerFactory = new DynamicTestPropertyContextCustomizerFactory();

	@Test
	void singleDynamicTestProperty() {
		// Arrange
		DynamicTestPropertyContextCustomizer expectedCustomizer = new DynamicTestPropertyContextCustomizer(
				Arrays.asList(TestPropertyValues.of("a=123")));
		// Act
		ContextCustomizer customizer = this.customizerFactory
				.createContextCustomizer(SingleTestPropertyClass.class, null);
		// Assert
		assertThat(customizer).isEqualTo(expectedCustomizer);
	}

	@Test
	void multipleDynamicTestProperty() {
		// Arrange
		DynamicTestPropertyContextCustomizer expectedCustomizer = new DynamicTestPropertyContextCustomizer(
				Arrays.asList(TestPropertyValues.of("a=123"),
						TestPropertyValues.of("b=456")));
		// Act
		ContextCustomizer customizer = this.customizerFactory
				.createContextCustomizer(MultipleTestPropertyClass.class, null);
		// Assert
		assertThat(customizer).isEqualTo(expectedCustomizer);
	}

	@Test
	void notStaticMethod() {

		ThrowableAssert.ThrowingCallable act = () -> this.customizerFactory
				.createContextCustomizer(ErrorWithNoStaticMethodClass.class, null);

		assertThatThrownBy(act).isInstanceOf(DynamicTestPropertyException.class)
				.hasMessage(
						"Annotation DynamicTestProperty must be used on a static method.");
	}

	@Test
	void wrongReturnTypeOfTheDynamicTestProperty() {

		ThrowableAssert.ThrowingCallable act = () -> this.customizerFactory
				.createContextCustomizer(ErrorWithWrongReturnTypeClass.class, null);

		assertThatThrownBy(act).isInstanceOf(DynamicTestPropertyException.class)
				.hasMessage(
						"DynamicTestProperty method must return the instance of TestPropertyValues.");
	}

	@Test
	void errorWhileTryingToGetTheValueOfProperties() {

		ThrowableAssert.ThrowingCallable act = () -> this.customizerFactory
				.createContextCustomizer(ErrorWhileRetrieveTheValueFromMethodClass.class,
						null);

		assertThatThrownBy(act).isInstanceOf(DynamicTestPropertyException.class)
				.hasMessage("Error while trying to get a value of dynamic properties.");
	}

	private static class SingleTestPropertyClass {

		@DynamicTestProperty
		private static TestPropertyValues getProps() {
			return TestPropertyValues.of("a=123");
		}

	}

	private static class MultipleTestPropertyClass {

		@DynamicTestProperty
		private static TestPropertyValues firstProps() {
			return TestPropertyValues.of("a=123");
		}

		@DynamicTestProperty
		private static TestPropertyValues secondProps() {
			return TestPropertyValues.of("b=456");
		}

	}

	private static class ErrorWithNoStaticMethodClass {

		@DynamicTestProperty
		private TestPropertyValues getProps() {
			return TestPropertyValues.of("a=123");
		}

	}

	private static class ErrorWithWrongReturnTypeClass {

		@DynamicTestProperty
		private static String getProps() {
			return "a=123";
		}

	}

	private static class ErrorWhileRetrieveTheValueFromMethodClass {

		@DynamicTestProperty
		private static TestPropertyValues getProps() {
			throw new RuntimeException("oops");
		}

	}

}
