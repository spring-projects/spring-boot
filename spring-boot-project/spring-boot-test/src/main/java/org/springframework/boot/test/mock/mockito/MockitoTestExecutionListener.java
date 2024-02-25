/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * {@link TestExecutionListener} to enable {@link MockBean @MockBean} and
 * {@link SpyBean @SpyBean} support. Also triggers
 * {@link MockitoAnnotations#openMocks(Object)} when any Mockito annotations used,
 * primarily to allow {@link Captor @Captor} annotations.
 * <p>
 * To use the automatic reset support of {@code @MockBean} and {@code @SpyBean}, configure
 * {@link ResetMocksTestExecutionListener} as well.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @since 1.4.2
 * @see ResetMocksTestExecutionListener
 */
public class MockitoTestExecutionListener extends AbstractTestExecutionListener {

	private static final String MOCKS_ATTRIBUTE_NAME = MockitoTestExecutionListener.class.getName() + ".mocks";

	/**
	 * Returns the order value for this test execution listener. The default value is
	 * 1950.
	 * @return the order value for this test execution listener
	 */
	@Override
	public final int getOrder() {
		return 1950;
	}

	/**
	 * Prepare the test instance before execution.
	 *
	 * This method is called before each test method is executed. It performs the
	 * following steps: 1. Closes any existing mocks to ensure a clean state. 2.
	 * Initializes the mocks for the test instance. 3. Injects any required fields into
	 * the test instance.
	 * @param testContext the test context containing information about the test instance
	 * @throws Exception if an error occurs during the preparation of the test instance
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		closeMocks(testContext);
		initMocks(testContext);
		injectFields(testContext);
	}

	/**
	 * This method is called before each test method is executed. It checks if the
	 * dependencies need to be reinjected and performs the necessary actions. If the
	 * "REINJECT_DEPENDENCIES_ATTRIBUTE" attribute is set to true in the test context, it
	 * closes the existing mocks, initializes new mocks, and reinjects the fields.
	 * @param testContext the test context containing information about the test being
	 * executed
	 * @throws Exception if an error occurs during the reinjection process
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		if (Boolean.TRUE.equals(
				testContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))) {
			closeMocks(testContext);
			initMocks(testContext);
			reinjectFields(testContext);
		}
	}

	/**
	 * Callback after a test method has been executed. This method is responsible for
	 * closing any mocks that were created during the test.
	 * @param testContext the test context containing information about the test
	 * @throws Exception if an error occurs while closing the mocks
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		closeMocks(testContext);
	}

	/**
	 * Cleans up and closes any mocks used during the test class.
	 * @param testContext the test context containing information about the test class
	 * @throws Exception if an error occurs during the cleanup process
	 */
	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		closeMocks(testContext);
	}

	/**
	 * Initializes the mocks for the given test context.
	 * @param testContext the test context to initialize the mocks for
	 */
	private void initMocks(TestContext testContext) {
		if (hasMockitoAnnotations(testContext)) {
			testContext.setAttribute(MOCKS_ATTRIBUTE_NAME, MockitoAnnotations.openMocks(testContext.getTestInstance()));
		}
	}

	/**
	 * Closes the mocks associated with the given test context.
	 * @param testContext the test context containing the mocks
	 * @throws Exception if an error occurs while closing the mocks
	 */
	private void closeMocks(TestContext testContext) throws Exception {
		Object mocks = testContext.getAttribute(MOCKS_ATTRIBUTE_NAME);
		if (mocks instanceof AutoCloseable closeable) {
			closeable.close();
		}
	}

	/**
	 * Checks if the given TestContext has any Mockito annotations.
	 * @param testContext the TestContext to check for Mockito annotations
	 * @return true if the TestContext has Mockito annotations, false otherwise
	 */
	private boolean hasMockitoAnnotations(TestContext testContext) {
		MockitoAnnotationCollection collector = new MockitoAnnotationCollection();
		ReflectionUtils.doWithFields(testContext.getTestClass(), collector);
		return collector.hasAnnotations();
	}

	/**
	 * Injects fields annotated with Mockito annotations in the given TestContext.
	 * @param testContext the TestContext containing the fields to be injected
	 */
	private void injectFields(TestContext testContext) {
		postProcessFields(testContext, (mockitoField, postProcessor) -> postProcessor.inject(mockitoField.field,
				mockitoField.target, mockitoField.definition));
	}

	/**
	 * Reinjects fields in the given TestContext.
	 * @param testContext the TestContext containing the fields to be reinjected
	 */
	private void reinjectFields(final TestContext testContext) {
		postProcessFields(testContext, (mockitoField, postProcessor) -> {
			ReflectionUtils.makeAccessible(mockitoField.field);
			ReflectionUtils.setField(mockitoField.field, testContext.getTestInstance(), null);
			postProcessor.inject(mockitoField.field, mockitoField.target, mockitoField.definition);
		});
	}

	/**
	 * Post-processes fields annotated with Mockito definitions.
	 * @param testContext The test context.
	 * @param consumer The consumer function to apply to each Mockito field and
	 * post-processor.
	 */
	private void postProcessFields(TestContext testContext, BiConsumer<MockitoField, MockitoPostProcessor> consumer) {
		DefinitionsParser parser = new DefinitionsParser();
		parser.parse(testContext.getTestClass());
		if (!parser.getDefinitions().isEmpty()) {
			MockitoPostProcessor postProcessor = testContext.getApplicationContext()
				.getBean(MockitoPostProcessor.class);
			for (Definition definition : parser.getDefinitions()) {
				Field field = parser.getField(definition);
				if (field != null) {
					consumer.accept(new MockitoField(field, testContext.getTestInstance(), definition), postProcessor);
				}
			}
		}
	}

	/**
	 * {@link FieldCallback} to collect Mockito annotations.
	 */
	private static final class MockitoAnnotationCollection implements FieldCallback {

		private final Set<Annotation> annotations = new LinkedHashSet<>();

		/**
		 * Iterates over the annotations declared on a given field and adds the
		 * annotations that belong to the Mockito framework to the collection of
		 * annotations.
		 * @param field the field to process
		 * @throws IllegalArgumentException if an error occurs while processing the field
		 */
		@Override
		public void doWith(Field field) throws IllegalArgumentException {
			for (Annotation annotation : field.getDeclaredAnnotations()) {
				if (annotation.annotationType().getName().startsWith("org.mockito")) {
					this.annotations.add(annotation);
				}
			}
		}

		/**
		 * Checks if the MockitoAnnotationCollection has any annotations.
		 * @return true if the MockitoAnnotationCollection has annotations, false
		 * otherwise.
		 */
		boolean hasAnnotations() {
			return !this.annotations.isEmpty();
		}

	}

	/**
	 * MockitoField class.
	 */
	private static final class MockitoField {

		private final Field field;

		private final Object target;

		private final Definition definition;

		/**
		 * Constructs a new MockitoField object with the specified field, instance, and
		 * definition.
		 * @param field the field to be set in the MockitoField object
		 * @param instance the instance to be set in the MockitoField object
		 * @param definition the definition to be set in the MockitoField object
		 */
		private MockitoField(Field field, Object instance, Definition definition) {
			this.field = field;
			this.target = instance;
			this.definition = definition;
		}

	}

}
