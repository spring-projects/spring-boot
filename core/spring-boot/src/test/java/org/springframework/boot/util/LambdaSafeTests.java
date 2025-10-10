/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.util.LambdaSafe.Filter;
import org.springframework.boot.util.LambdaSafe.InvocationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LambdaSafe}.
 *
 * @author Phillip Webb
 */
class LambdaSafeTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void callbackWhenCallbackTypeIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LambdaSafe.callback(null, new Object(), null))
			.withMessageContaining("'callbackType' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void callbackWhenCallbackInstanceIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LambdaSafe.callback(Object.class, null, null))
			.withMessageContaining("'callbackInstance' must not be null");
	}

	@Test
	void callbackInvokeWhenNoGenericShouldInvokeCallback() {
		NonGenericCallback callbackInstance = mock(NonGenericCallback.class);
		String argument = "foo";
		LambdaSafe.callback(NonGenericCallback.class, callbackInstance, argument).invoke((c) -> c.handle(argument));
		then(callbackInstance).should().handle(argument);
	}

	@Test
	@SuppressWarnings("unchecked")
	void callbackInvokeWhenHasGenericShouldInvokeCallback() {
		StringCallback callbackInstance = mock(StringCallback.class);
		String argument = "foo";
		LambdaSafe.callback(GenericCallback.class, callbackInstance, argument).invoke((c) -> c.handle(argument));
		then(callbackInstance).should().handle(argument);
	}

	@Test
	@SuppressWarnings("unchecked")
	void callbackInvokeWhenHasResolvableGenericMatchShouldInvokeCallback() {
		StringBuilderCallback callbackInstance = mock(StringBuilderCallback.class);
		StringBuilder argument = new StringBuilder("foo");
		LambdaSafe.callback(GenericCallback.class, callbackInstance, argument).invoke((c) -> c.handle(argument));
		then(callbackInstance).should().handle(argument);
	}

	@Test
	@SuppressWarnings("unchecked")
	void callbackInvokeWhenHasResolvableGenericNonMatchShouldNotInvokeCallback() {
		GenericCallback<?> callbackInstance = mock(StringBuilderCallback.class);
		String argument = "foo";
		LambdaSafe.callback(GenericCallback.class, callbackInstance, argument).invoke((c) -> c.handle(argument));
		then(callbackInstance).shouldHaveNoInteractions();
	}

	@Test
	@SuppressWarnings("unchecked")
	void callbackInvokeWhenLambdaMismatchShouldSwallowException() {
		GenericCallback<StringBuilder> callbackInstance = (s) -> fail("Should not get here");
		String argument = "foo";
		LambdaSafe.callback(GenericCallback.class, callbackInstance, argument).invoke((c) -> c.handle(argument));
	}

	@Test
	@SuppressWarnings("unchecked")
	void callbackInvokeWhenLambdaMismatchOnDifferentArgumentShouldSwallowException() {
		GenericMultiArgCallback<StringBuilder> callbackInstance = (n, s, b) -> fail("Should not get here");
		String argument = "foo";
		LambdaSafe.callback(GenericMultiArgCallback.class, callbackInstance, argument)
			.invoke((c) -> c.handle(1, argument, false));
	}

	@Test
	void callbackInvokeAndWhenNoGenericShouldReturnResult() {
		NonGenericFactory callbackInstance = mock(NonGenericFactory.class);
		String argument = "foo";
		given(callbackInstance.handle("foo")).willReturn(123);
		Function<NonGenericFactory, @Nullable Integer> handle = (c) -> c.handle(argument);
		InvocationResult<Integer> result = LambdaSafe.callback(NonGenericFactory.class, callbackInstance, argument)
			.invokeAnd(handle);
		assertThat(result.hasResult()).isTrue();
		assertThat(result.get()).isEqualTo(123);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbackInvokeAndWhenHasGenericShouldReturnResult() {
		StringFactory callbackInstance = mock(StringFactory.class);
		String argument = "foo";
		given(callbackInstance.handle("foo")).willReturn(123);
		Function<GenericFactory, @Nullable Integer> handle = (c) -> c.handle(argument);
		InvocationResult<Integer> result = LambdaSafe.callback(GenericFactory.class, callbackInstance, argument)
			.invokeAnd(handle);
		assertThat(result.hasResult()).isTrue();
		assertThat(result.get()).isEqualTo(123);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbackInvokeAndWhenReturnNullShouldReturnResult() {
		StringFactory callbackInstance = mock(StringFactory.class);
		String argument = "foo";
		given(callbackInstance.handle("foo")).willReturn(null);
		Function<GenericFactory, @Nullable Integer> handle = (c) -> c.handle(argument);
		InvocationResult<Integer> result = LambdaSafe.callback(GenericFactory.class, callbackInstance, argument)
			.invokeAnd(handle);
		assertThat(result.hasResult()).isTrue();
		assertThat(result.get()).isNull();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbackInvokeAndWhenHasResolvableGenericMatchShouldReturnResult() {
		StringBuilderFactory callbackInstance = mock(StringBuilderFactory.class);
		StringBuilder argument = new StringBuilder("foo");
		given(callbackInstance.handle(any(StringBuilder.class))).willReturn(123);
		Function<GenericFactory, @Nullable Integer> handle = (c) -> c.handle(argument);
		InvocationResult<Integer> result = LambdaSafe.callback(GenericFactory.class, callbackInstance, argument)
			.invokeAnd(handle);
		then(callbackInstance).should().handle(argument);
		assertThat(result.hasResult()).isTrue();
		assertThat(result.get()).isEqualTo(123);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbackInvokeAndWhenHasResolvableGenericNonMatchShouldReturnNoResult() {
		GenericFactory<?> callbackInstance = mock(StringBuilderFactory.class);
		String argument = "foo";
		Function<GenericFactory, @Nullable Integer> handle = (c) -> c.handle(argument);
		InvocationResult<Integer> result = LambdaSafe.callback(GenericFactory.class, callbackInstance, argument)
			.invokeAnd(handle);
		assertThat(result.hasResult()).isFalse();
		then(callbackInstance).shouldHaveNoInteractions();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbackInvokeAndWhenLambdaMismatchShouldSwallowException() {
		GenericFactory<StringBuilder> callbackInstance = (s) -> {
			fail("Should not get here");
			return 123;
		};
		String argument = "foo";
		Function<GenericFactory, @Nullable Integer> handle = (c) -> c.handle(argument);
		InvocationResult<Integer> result = LambdaSafe.callback(GenericFactory.class, callbackInstance, argument)
			.invokeAnd(handle);
		assertThat(result.hasResult()).isFalse();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbackInvokeAndWhenLambdaMismatchOnDifferentArgumentShouldSwallowException() {
		GenericMultiArgFactory<StringBuilder> callbackInstance = (n, s, b) -> {
			fail("Should not get here");
			return 123;
		};
		String argument = "foo";
		Function<GenericMultiArgFactory, @Nullable Integer> handle = (c) -> c.handle(1, argument, false);
		InvocationResult<Integer> result = LambdaSafe.callback(GenericMultiArgFactory.class, callbackInstance, argument)
			.invokeAnd(handle);
		assertThat(result.hasResult()).isFalse();
	}

	@Test
	void callbacksInvokeWhenNoGenericShouldInvokeCallbacks() {
		NonGenericCallback callbackInstance = mock(NonGenericCallback.class);
		String argument = "foo";
		Consumer<NonGenericCallback> handle = (c) -> c.handle(argument);
		LambdaSafe.callbacks(NonGenericCallback.class, Collections.singleton(callbackInstance), argument)
			.invoke(handle);
		then(callbackInstance).should().handle(argument);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbacksInvokeWhenHasGenericShouldInvokeCallback() {
		StringCallback callbackInstance = mock(StringCallback.class);
		String argument = "foo";
		Consumer<GenericCallback> handle = (c) -> c.handle(argument);
		LambdaSafe.callbacks(GenericCallback.class, Collections.singleton(callbackInstance), argument).invoke(handle);
		then(callbackInstance).should().handle(argument);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbacksInvokeWhenHasResolvableGenericMatchShouldInvokeCallback() {
		StringBuilderCallback callbackInstance = mock(StringBuilderCallback.class);
		StringBuilder argument = new StringBuilder("foo");
		Consumer<GenericCallback> handle = (c) -> c.handle(argument);
		LambdaSafe.callbacks(GenericCallback.class, Collections.singleton(callbackInstance), argument).invoke(handle);
		then(callbackInstance).should().handle(argument);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbacksInvokeWhenHasResolvableGenericNonMatchShouldNotInvokeCallback() {
		GenericCallback<?> callbackInstance = mock(StringBuilderCallback.class);
		String argument = "foo";
		Consumer<GenericCallback> handle = (c) -> c.handle(null);
		LambdaSafe.callbacks(GenericCallback.class, Collections.singleton(callbackInstance), argument).invoke(handle);
		then(callbackInstance).shouldHaveNoInteractions();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbacksInvokeWhenLambdaMismatchShouldSwallowException() {
		GenericCallback<StringBuilder> callbackInstance = (s) -> fail("Should not get here");
		String argument = "foo";
		Consumer<GenericCallback> handle = (c) -> c.handle(argument);
		LambdaSafe.callbacks(GenericCallback.class, Collections.singleton(callbackInstance), argument).invoke(handle);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbacksInvokeWhenLambdaMismatchOnDifferentArgumentShouldSwallowException() {
		GenericMultiArgCallback<StringBuilder> callbackInstance = (n, s, b) -> fail("Should not get here");
		String argument = "foo";
		Consumer<GenericMultiArgCallback> handle = (c) -> c.handle(1, argument, false);
		LambdaSafe.callbacks(GenericMultiArgCallback.class, Collections.singleton(callbackInstance), argument)
			.invoke(handle);
	}

	@Test
	void callbacksInvokeAndWhenNoGenericShouldReturnResult() {
		NonGenericFactory callbackInstance = mock(NonGenericFactory.class);
		String argument = "foo";
		given(callbackInstance.handle("foo")).willReturn(123);
		Function<NonGenericFactory, @Nullable Integer> handle = (c) -> c.handle(argument);
		Stream<Integer> result = LambdaSafe
			.callbacks(NonGenericFactory.class, Collections.singleton(callbackInstance), argument)
			.invokeAnd(handle);
		assertThat(result).containsExactly(123);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbacksInvokeAndWhenHasGenericShouldReturnResult() {
		StringFactory callbackInstance = mock(StringFactory.class);
		String argument = "foo";
		given(callbackInstance.handle("foo")).willReturn(123);
		Function<GenericFactory, @Nullable Integer> handle = (c) -> c.handle(argument);
		Stream<Integer> result = LambdaSafe
			.callbacks(GenericFactory.class, Collections.singleton(callbackInstance), argument)
			.invokeAnd(handle);
		assertThat(result).containsExactly(123);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbacksInvokeAndWhenReturnNullShouldReturnResult() {
		StringFactory callbackInstance = mock(StringFactory.class);
		String argument = "foo";
		given(callbackInstance.handle("foo")).willReturn(null);
		Function<GenericFactory, @Nullable Integer> handle = (c) -> c.handle(argument);
		Stream<Integer> result = LambdaSafe
			.callbacks(GenericFactory.class, Collections.singleton(callbackInstance), argument)
			.invokeAnd(handle);
		assertThat(result).containsExactly((Integer) null);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbacksInvokeAndWhenHasResolvableGenericMatchShouldReturnResult() {
		StringBuilderFactory callbackInstance = mock(StringBuilderFactory.class);
		StringBuilder argument = new StringBuilder("foo");
		given(callbackInstance.handle(any(StringBuilder.class))).willReturn(123);
		Function<GenericFactory, @Nullable Integer> handle = (c) -> c.handle(argument);
		Stream<Integer> result = LambdaSafe
			.callbacks(GenericFactory.class, Collections.singleton(callbackInstance), argument)
			.invokeAnd(handle);
		assertThat(result).containsExactly(123);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbacksInvokeAndWhenHasResolvableGenericNonMatchShouldReturnNoResult() {
		GenericFactory<?> callbackInstance = mock(StringBuilderFactory.class);
		String argument = "foo";
		Function<GenericFactory, @Nullable Integer> handle = (c) -> c.handle(argument);
		Stream<Integer> result = LambdaSafe
			.callbacks(GenericFactory.class, Collections.singleton(callbackInstance), argument)
			.invokeAnd(handle);
		assertThat(result).isEmpty();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbacksInvokeAndWhenLambdaMismatchShouldSwallowException() {
		GenericFactory<StringBuilder> callbackInstance = (s) -> {
			fail("Should not get here");
			return 123;
		};
		String argument = "foo";
		Function<GenericFactory, @Nullable Integer> handle = (c) -> (c).handle(argument);
		Stream<Integer> result = LambdaSafe
			.callbacks(GenericFactory.class, Collections.singleton(callbackInstance), argument)
			.invokeAnd(handle);
		assertThat(result).isEmpty();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbacksInvokeAndWhenLambdaMismatchOnDifferentArgumentShouldSwallowException() {
		GenericMultiArgFactory<StringBuilder> callbackInstance = (n, s, b) -> {
			fail("Should not get here");
			return 123;
		};
		String argument = "foo";
		Function<GenericMultiArgFactory, @Nullable Integer> handle = (c) -> c.handle(1, argument, false);
		Stream<Integer> result = LambdaSafe
			.callbacks(GenericMultiArgFactory.class, Collections.singleton(callbackInstance), argument)
			.invokeAnd(handle);
		assertThat(result).isEmpty();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void callbacksInvokeWhenMultipleShouldInvokeSuitable() {
		List<GenericFactory<?>> callbackInstances = new ArrayList<>();
		GenericFactory<String> callback1 = (s) -> 1;
		GenericFactory<CharSequence> callback2 = (s) -> 2;
		GenericFactory<StringBuilder> callback3 = (s) -> 3;
		StringFactory callback4 = mock(StringFactory.class);
		given(callback4.handle(any(String.class))).willReturn(4);
		StringBuilderFactory callback5 = mock(StringBuilderFactory.class);
		given(callback5.handle(any(StringBuilder.class))).willReturn(5);
		callbackInstances.add(callback1);
		callbackInstances.add(callback2);
		callbackInstances.add(callback3);
		callbackInstances.add(callback4);
		callbackInstances.add(callback5);
		String argument = "foo";
		Function<GenericFactory, @Nullable Integer> handle = (c) -> c.handle(argument);
		Stream<Integer> result = LambdaSafe.callbacks(GenericFactory.class, callbackInstances, argument)
			.invokeAnd(handle);
		assertThat(result).containsExactly(1, 2, 4);
	}

	@Test
	@SuppressWarnings("unchecked")
	void callbackWithFilterShouldUseFilter() {
		GenericCallback<?> callbackInstance = mock(StringBuilderCallback.class);
		String argument = "foo";
		LambdaSafe.callback(GenericCallback.class, callbackInstance, argument)
			.withFilter(Filter.allowAll())
			.invoke((c) -> c.handle(null));
		then(callbackInstance).should().handle(null);
	}

	@Test
	@SuppressWarnings("unchecked")
	void callbackWithLoggerShouldUseLogger() {
		Log logger = mock(Log.class);
		given(logger.isDebugEnabled()).willReturn(true);
		GenericCallback<StringBuilder> callbackInstance = (s) -> fail("Should not get here");
		String argument = "foo";
		LambdaSafe.callback(GenericCallback.class, callbackInstance, argument)
			.withLogger(logger)
			.invoke((c) -> c.handle(argument));
		then(logger).should()
			.debug(contains("Non-matching CharSequence type for callback LambdaSafeTests.GenericCallback"),
					any(Throwable.class));
	}

	interface NonGenericCallback {

		void handle(String argument);

	}

	interface GenericCallback<T extends CharSequence> {

		void handle(@Nullable T argument);

	}

	interface StringCallback extends GenericCallback<String> {

	}

	interface StringBuilderCallback extends GenericCallback<StringBuilder> {

	}

	interface GenericMultiArgCallback<T extends CharSequence> {

		void handle(Integer number, T argument, Boolean bool);

	}

	interface NonGenericFactory {

		Integer handle(String argument);

	}

	interface GenericFactory<T extends CharSequence> {

		Integer handle(T argument);

	}

	interface StringFactory extends GenericFactory<String> {

	}

	interface StringBuilderFactory extends GenericFactory<StringBuilder> {

	}

	interface GenericMultiArgFactory<T extends CharSequence> {

		Integer handle(Integer number, T argument, Boolean bool);

	}

}
