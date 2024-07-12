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

import java.lang.reflect.Field;
import java.util.function.BiConsumer;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.ReflectionUtils;

/**
 * {@link TestExecutionListener} to enable {@link MockBean @MockBean} and
 * {@link SpyBean @SpyBean} support.
 * <p>
 * To use the automatic reset support of {@code @MockBean} and {@code @SpyBean}, configure
 * {@link ResetMocksTestExecutionListener} as well.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @since 1.4.2
 * @see ResetMocksTestExecutionListener
 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of
 * {@link org.springframework.test.context.bean.override.mockito.MockitoTestExecutionListener}
 */
@SuppressWarnings("removal")
@Deprecated(since = "3.4.0", forRemoval = true)
public class MockitoTestExecutionListener extends AbstractTestExecutionListener {

	@Override
	public final int getOrder() {
		return 1950;
	}

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		injectFields(testContext);
	}

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		if (Boolean.TRUE.equals(
				testContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))) {
			reinjectFields(testContext);
		}
	}

	private void injectFields(TestContext testContext) {
		postProcessFields(testContext, (mockitoField, postProcessor) -> postProcessor.inject(mockitoField.field,
				mockitoField.target, mockitoField.definition));
	}

	private void reinjectFields(final TestContext testContext) {
		postProcessFields(testContext, (mockitoField, postProcessor) -> {
			ReflectionUtils.makeAccessible(mockitoField.field);
			ReflectionUtils.setField(mockitoField.field, testContext.getTestInstance(), null);
			postProcessor.inject(mockitoField.field, mockitoField.target, mockitoField.definition);
		});
	}

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

	private static final class MockitoField {

		private final Field field;

		private final Object target;

		private final Definition definition;

		private MockitoField(Field field, Object instance, Definition definition) {
			this.field = field;
			this.target = instance;
			this.definition = definition;
		}

	}

}
