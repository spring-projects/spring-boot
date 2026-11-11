/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.pulsar.autoconfigure;

import java.util.List;
import java.util.function.BiConsumer;

import org.assertj.core.api.AssertDelegateTarget;
import org.mockito.InOrder;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
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
		Object field = ReflectionTestUtils.getField(source, fieldName);
		assertThat(field).isNotNull();
		return new CustomizersAssert(field);
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
