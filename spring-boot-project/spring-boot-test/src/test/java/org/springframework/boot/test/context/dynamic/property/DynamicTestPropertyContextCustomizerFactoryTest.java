package org.springframework.boot.test.context.dynamic.property;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.test.context.ContextCustomizer;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Tests for {@link DynamicTestPropertyContextCustomizerFactory}
 *
 * @author Anatoliy Korovin
 */
class DynamicTestPropertyContextCustomizerFactoryTest {

	private DynamicTestPropertyContextCustomizerFactory customizerFactory =
			new DynamicTestPropertyContextCustomizerFactory();


	@Test
	void singleDynamicTestProperty() {
		// Arrange
		DynamicTestPropertyContextCustomizer expectedCustomizer =
				new DynamicTestPropertyContextCustomizer(Arrays.asList(TestPropertyValues.of("a=123")));
		// Act
		ContextCustomizer customizer =
				customizerFactory.createContextCustomizer(SingleTestPropertyClass.class, null);
		// Assert
		assertThat(customizer).isEqualTo(expectedCustomizer);
	}

	@Test
	void multipleDynamicTestProperty() {
		// Arrange
		DynamicTestPropertyContextCustomizer expectedCustomizer =
				new DynamicTestPropertyContextCustomizer(Arrays.asList(TestPropertyValues.of("a=123"),
																	   TestPropertyValues.of("b=456")));
		// Act
		ContextCustomizer customizer =
				customizerFactory.createContextCustomizer(MultipleTestPropertyClass.class, null);
		// Assert
		assertThat(customizer).isEqualTo(expectedCustomizer);
	}

	@Test
	void notStaticMethod() {
		DynamicTestPropertyException e =
				assertThrows(DynamicTestPropertyException.class,
							 () -> customizerFactory.createContextCustomizer(ErrorWithNoStaticMethodClass.class, null));

		assertThat(e.getMessage()).contains("Annotation DynamicTestProperty must be used on a static method");
	}

	@Test
	void wrongReturnTypeOfTheDynamicTestProperty() {

		DynamicTestPropertyException e =
				assertThrows(DynamicTestPropertyException.class,
							 () -> customizerFactory.createContextCustomizer(ErrorWithWrongReturnTypeClass.class, null));

		assertThat(e.getMessage()).contains("DynamicTestProperty method must return the instance of TestPropertyValues");
	}

	@Test
	void errorWhileTryingToGetTheValueOfProperties() {

		DynamicTestPropertyException e =
				assertThrows(DynamicTestPropertyException.class,
							 () -> customizerFactory.createContextCustomizer(ErrorWhileRetrieveTheValueFromMethodClass.class, null));

		assertThat(e.getMessage()).contains("Error while trying to get a value of dynamic properties");
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