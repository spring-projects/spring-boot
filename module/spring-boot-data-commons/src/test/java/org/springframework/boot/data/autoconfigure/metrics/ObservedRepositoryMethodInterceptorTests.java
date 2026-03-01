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

package org.springframework.boot.data.autoconfigure.metrics;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.repository.Repository;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ObservedRepositoryMethodInterceptor}.
 *
 * @author Kwonneung Lee
 */
class ObservedRepositoryMethodInterceptorTests {

	private ObservationRegistry observationRegistry;

	private RecordingObservationHandler handler;

	@BeforeEach
	void setup() {
		this.observationRegistry = ObservationRegistry.create();
		this.handler = new RecordingObservationHandler();
		this.observationRegistry.observationConfig().observationHandler(this.handler);
	}

	@Test
	void invokeWhenNoObservedAnnotationProceedsWithoutObservation() throws Throwable {
		ObservedRepositoryMethodInterceptor interceptor = createInterceptor(NoAnnotationsRepository.class);
		MethodInvocation invocation = mockInvocation(NoAnnotationsRepository.class, "findById");
		given(invocation.proceed()).willReturn("result");
		Object result = interceptor.invoke(invocation);
		assertThat(result).isEqualTo("result");
		assertThat(this.handler.observationNames).isEmpty();
	}

	@Test
	void invokeWhenObservedOnClassCreatesObservation() throws Throwable {
		ObservedRepositoryMethodInterceptor interceptor = createInterceptor(ObservedClassRepository.class);
		MethodInvocation invocation = mockInvocation(ObservedClassRepository.class, "findById");
		given(invocation.proceed()).willReturn("result");
		Object result = interceptor.invoke(invocation);
		assertThat(result).isEqualTo("result");
		assertThat(this.handler.observationNames).containsExactly("class.repository");
	}

	@Test
	void invokeWhenObservedOnMethodCreatesObservation() throws Throwable {
		ObservedRepositoryMethodInterceptor interceptor = createInterceptor(ObservedMethodRepository.class);
		MethodInvocation invocation = mockInvocation(ObservedMethodRepository.class, "findById");
		given(invocation.proceed()).willReturn("result");
		Object result = interceptor.invoke(invocation);
		assertThat(result).isEqualTo("result");
		assertThat(this.handler.observationNames).containsExactly("method.repository");
	}

	@Test
	void invokeWhenObservedOnMethodAndClassCreatesMethodObservation() throws Throwable {
		ObservedRepositoryMethodInterceptor interceptor = createInterceptor(ObservedMethodAndClassRepository.class);
		MethodInvocation invocation = mockInvocation(ObservedMethodAndClassRepository.class, "findById");
		given(invocation.proceed()).willReturn("result");
		Object result = interceptor.invoke(invocation);
		assertThat(result).isEqualTo("result");
		assertThat(this.handler.observationNames).containsExactly("method.repository");
	}

	@Test
	void invokeWhenLowCardinalityKeyValuesNotInPairsThrowsException() {
		ObservedRepositoryMethodInterceptor interceptor = createInterceptor(ObservedInvalidLowCardinalityRepository.class);
		MethodInvocation invocation = mockInvocation(ObservedInvalidLowCardinalityRepository.class, "findById");
		assertThatThrownBy(() -> interceptor.invoke(invocation))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("'lowCardinalityKeyValues' must contain an even number of entries");
	}

	@Test
	void invokeRecordsSuccessState() throws Throwable {
		ObservedRepositoryMethodInterceptor interceptor = createInterceptor(ObservedClassRepository.class);
		MethodInvocation invocation = mockInvocation(ObservedClassRepository.class, "findById");
		given(invocation.proceed()).willReturn("result");
		interceptor.invoke(invocation);
		assertThat(this.handler.lastContext).isNotNull();
		assertKeyValue(this.handler.lastContext, "state", "SUCCESS");
	}

	@Test
	void invokeRecordsErrorState() throws Throwable {
		ObservedRepositoryMethodInterceptor interceptor = createInterceptor(ObservedClassRepository.class);
		MethodInvocation invocation = mockInvocation(ObservedClassRepository.class, "findById");
		given(invocation.proceed()).willThrow(new RuntimeException("test error"));
		assertThatThrownBy(() -> interceptor.invoke(invocation)).isInstanceOf(RuntimeException.class)
			.hasMessage("test error");
		assertThat(this.handler.lastContext).isNotNull();
		assertKeyValue(this.handler.lastContext, "state", "ERROR");
		assertThat(this.handler.lastContext.getError()).isInstanceOf(RuntimeException.class);
	}

	@Test
	void invokeRecordsClassAndMethodKeyValues() throws Throwable {
		ObservedRepositoryMethodInterceptor interceptor = createInterceptor(ObservedClassRepository.class);
		MethodInvocation invocation = mockInvocation(ObservedClassRepository.class, "findById");
		given(invocation.proceed()).willReturn("result");
		interceptor.invoke(invocation);
		assertThat(this.handler.lastContext).isNotNull();
		assertKeyValue(this.handler.lastContext, "class", "ObservedClassRepository");
		assertKeyValue(this.handler.lastContext, "method", "findById");
	}

	private void assertKeyValue(Observation.Context context, String key, String expectedValue) {
		KeyValue keyValue = context.getLowCardinalityKeyValue(key);
		assertThat(keyValue).isNotNull();
		assertThat(keyValue.getValue()).isEqualTo(expectedValue);
	}

	private ObservedRepositoryMethodInterceptor createInterceptor(Class<?> repositoryInterface) {
		return new ObservedRepositoryMethodInterceptor(() -> this.observationRegistry, repositoryInterface);
	}

	private MethodInvocation mockInvocation(Class<?> repositoryInterface, String methodName) {
		Method method = ReflectionUtils.findMethod(repositoryInterface, methodName, long.class);
		assertThat(method).isNotNull();
		MethodInvocation invocation = mock(MethodInvocation.class);
		given(invocation.getMethod()).willReturn(method);
		return invocation;
	}

	interface NoAnnotationsRepository extends Repository<Example, Long> {

		Example findById(long id);

	}

	@Observed(name = "class.repository")
	interface ObservedClassRepository extends Repository<Example, Long> {

		Example findById(long id);

	}

	interface ObservedMethodRepository extends Repository<Example, Long> {

		@Observed(name = "method.repository")
		Example findById(long id);

	}

	@Observed(name = "class.repository")
	interface ObservedMethodAndClassRepository extends Repository<Example, Long> {

		@Observed(name = "method.repository")
		Example findById(long id);

	}

	@Observed(name = "class.repository", lowCardinalityKeyValues = { "key", "value", "orphan" })
	interface ObservedInvalidLowCardinalityRepository extends Repository<Example, Long> {

		Example findById(long id);

	}

	static class Example {

	}

	private static class RecordingObservationHandler implements ObservationHandler<Observation.Context> {

		final List<String> observationNames = new ArrayList<>();

		Observation.@Nullable Context lastContext;

		@Override
		public void onStart(Observation.Context context) {
			this.observationNames.add(context.getName());
		}

		@Override
		public void onStop(Observation.Context context) {
			this.lastContext = context;
		}

		@Override
		public boolean supportsContext(Observation.Context context) {
			return true;
		}

	}

}
