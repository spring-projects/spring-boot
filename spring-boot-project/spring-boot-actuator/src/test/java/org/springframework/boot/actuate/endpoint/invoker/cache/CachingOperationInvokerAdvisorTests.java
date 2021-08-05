/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.invoker.cache;

import java.lang.reflect.Method;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameters;
import org.springframework.boot.actuate.endpoint.invoke.reflect.OperationMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CachingOperationInvokerAdvisor}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@ExtendWith(MockitoExtension.class)
class CachingOperationInvokerAdvisorTests {

	@Mock
	private OperationInvoker invoker;

	@Mock
	private Function<EndpointId, Long> timeToLive;

	private CachingOperationInvokerAdvisor advisor;

	@BeforeEach
	void setup() {
		this.advisor = new CachingOperationInvokerAdvisor(this.timeToLive);
	}

	@Test
	void applyWhenOperationIsNotReadShouldNotAddAdvise() {
		OperationParameters parameters = getParameters("get");
		OperationInvoker advised = this.advisor.apply(EndpointId.of("foo"), OperationType.WRITE, parameters,
				this.invoker);
		assertThat(advised).isSameAs(this.invoker);
	}

	@Test
	void applyWhenHasAtLeaseOneMandatoryParameterShouldNotAddAdvise() {
		OperationParameters parameters = getParameters("getWithParameters", String.class, String.class);
		OperationInvoker advised = this.advisor.apply(EndpointId.of("foo"), OperationType.READ, parameters,
				this.invoker);
		assertThat(advised).isSameAs(this.invoker);
	}

	@Test
	void applyWhenTimeToLiveReturnsNullShouldNotAddAdvise() {
		OperationParameters parameters = getParameters("get");
		given(this.timeToLive.apply(any())).willReturn(null);
		OperationInvoker advised = this.advisor.apply(EndpointId.of("foo"), OperationType.READ, parameters,
				this.invoker);
		assertThat(advised).isSameAs(this.invoker);
		verify(this.timeToLive).apply(EndpointId.of("foo"));
	}

	@Test
	void applyWhenTimeToLiveIsZeroShouldNotAddAdvise() {
		OperationParameters parameters = getParameters("get");
		given(this.timeToLive.apply(any())).willReturn(0L);
		OperationInvoker advised = this.advisor.apply(EndpointId.of("foo"), OperationType.READ, parameters,
				this.invoker);
		assertThat(advised).isSameAs(this.invoker);
		verify(this.timeToLive).apply(EndpointId.of("foo"));
	}

	@Test
	void applyShouldAddCacheAdvise() {
		OperationParameters parameters = getParameters("get");
		given(this.timeToLive.apply(any())).willReturn(100L);
		assertAdviseIsApplied(parameters);
	}

	@Test
	void applyWithAllOptionalParametersShouldAddAdvise() {
		OperationParameters parameters = getParameters("getWithAllOptionalParameters", String.class, String.class);
		given(this.timeToLive.apply(any())).willReturn(100L);
		assertAdviseIsApplied(parameters);
	}

	@Test
	void applyWithSecurityContextShouldAddAdvise() {
		OperationParameters parameters = getParameters("getWithSecurityContext", SecurityContext.class, String.class);
		given(this.timeToLive.apply(any())).willReturn(100L);
		assertAdviseIsApplied(parameters);
	}

	@Test
	void applyWithApiVersionShouldAddAdvise() {
		OperationParameters parameters = getParameters("getWithApiVersion", ApiVersion.class, String.class);
		given(this.timeToLive.apply(any())).willReturn(100L);
		assertAdviseIsApplied(parameters);
	}

	private void assertAdviseIsApplied(OperationParameters parameters) {
		OperationInvoker advised = this.advisor.apply(EndpointId.of("foo"), OperationType.READ, parameters,
				this.invoker);
		assertThat(advised).isInstanceOf(CachingOperationInvoker.class);
		assertThat(advised).hasFieldOrPropertyWithValue("invoker", this.invoker);
		assertThat(advised).hasFieldOrPropertyWithValue("timeToLive", 100L);
	}

	private OperationParameters getParameters(String methodName, Class<?>... parameterTypes) {
		return getOperationMethod(methodName, parameterTypes).getParameters();
	}

	private OperationMethod getOperationMethod(String methodName, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(TestOperations.class, methodName, parameterTypes);
		return new OperationMethod(method, OperationType.READ);
	}

	static class TestOperations {

		String get() {
			return "";
		}

		String getWithParameters(@Nullable String foo, String bar) {
			return "";
		}

		String getWithAllOptionalParameters(@Nullable String foo, @Nullable String bar) {
			return "";
		}

		String getWithSecurityContext(SecurityContext securityContext, @Nullable String bar) {
			return "";
		}

		String getWithApiVersion(ApiVersion apiVersion, @Nullable String bar) {
			return "";
		}

	}

}
