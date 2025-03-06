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

package org.springframework.boot.autoconfigure.pulsar;

import java.util.List;
import java.util.function.BiConsumer;

import org.assertj.core.api.AssertDelegateTarget;
import org.mockito.InOrder;

import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Test utility used to check customizers are called correctly.
 *
 * @param <C> the customizer type
 * @param <T> the target class that is customized
 * @author Phillip Webb
 * @author Chris Bono
 */
final class Customizers<C, T> {

	private final BiConsumer<C, T> customizeAction;

	private final Class<T> targetClass;

	@SuppressWarnings("unchecked")
	private Customizers(Class<?> targetClass, BiConsumer<C, T> customizeAction) {
		this.customizeAction = customizeAction;
		this.targetClass = (Class<T>) targetClass;
	}

	/**
	 * Create an instance by getting the value from a field.
	 * @param source the source to extract the customizers from
	 * @param fieldName the field name
	 * @return a new {@link CustomizersAssert} instance
	 */
	@SuppressWarnings("unchecked")
	CustomizersAssert fromField(Object source, String fieldName) {
		return new CustomizersAssert(ReflectionTestUtils.getField(source, fieldName));
	}

	/**
	 * Create a new {@link Customizers} instance.
	 * @param <C> the customizer class
	 * @param <T> the target class that is customized
	 * @param targetClass the target class that is customized
	 * @param customizeAction the customizer action to take
	 * @return a new {@link Customizers} instance
	 */
	static <C, T> Customizers<C, T> of(Class<?> targetClass, BiConsumer<C, T> customizeAction) {
		return new Customizers<>(targetClass, customizeAction);
	}

	/**
	 * Assertions that can be applied to customizers.
	 */
	final class CustomizersAssert implements AssertDelegateTarget {

		private final List<C> customizers;

		@SuppressWarnings("unchecked")
		private CustomizersAssert(Object customizers) {
			this.customizers = (customizers instanceof List) ? (List<C>) customizers : List.of((C) customizers);
		}

		/**
		 * Assert that the customize method is called in a specified order. It is expected
		 * that each customizer has set a unique value so the expected values can be used
		 * as a verify step.
		 * @param <V> the value type
		 * @param call the call the customizer makes
		 * @param expectedValues the expected values
		 */
		@SuppressWarnings("unchecked")
		<V> void callsInOrder(BiConsumer<T, V> call, V... expectedValues) {
			T target = mock(Customizers.this.targetClass);
			BiConsumer<C, T> customizeAction = Customizers.this.customizeAction;
			this.customizers.forEach((customizer) -> customizeAction.accept(customizer, target));
			InOrder ordered = inOrder(target);
			for (V expectedValue : expectedValues) {
				call.accept(ordered.verify(target), expectedValue);
			}
		}

	}

}
