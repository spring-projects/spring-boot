/*
 * Copyright 2012-2022 the original author or authors.
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

import java.security.Principal;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.OperationArgumentResolver;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link CachingOperationInvoker}.
 *
 * @author Stephane Nicoll
 * @author Christoph Dreis
 * @author Phillip Webb
 */
class CachingOperationInvokerTests {

	private static final long CACHE_TTL = Duration.ofHours(1).toMillis();

	@Test
	void createInstanceWithTtlSetToZero() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new CachingOperationInvoker(mock(OperationInvoker.class), 0))
				.withMessageContaining("TimeToLive");
	}

	@Test
	void cacheInTtlRangeWithNoParameter() {
		assertCacheIsUsed(Collections.emptyMap());
	}

	@Test
	void cacheInTtlWithPrincipal() {
		assertCacheIsUsed(Collections.emptyMap(), mock(Principal.class));
	}

	@Test
	void cacheInTtlWithNullParameters() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("first", null);
		parameters.put("second", null);
		assertCacheIsUsed(parameters);
	}

	@Test
	void cacheInTtlWithMonoResponse() {
		MonoOperationInvoker.invocations = new AtomicInteger();
		MonoOperationInvoker target = new MonoOperationInvoker();
		InvocationContext context = new InvocationContext(mock(SecurityContext.class), Collections.emptyMap());
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, CACHE_TTL);
		Object response = ((Mono<?>) invoker.invoke(context)).block();
		Object cachedResponse = ((Mono<?>) invoker.invoke(context)).block();
		assertThat(MonoOperationInvoker.invocations).hasValue(1);
		assertThat(response).isSameAs(cachedResponse);
	}

	@Test
	void cacheInTtlWithFluxResponse() {
		FluxOperationInvoker.invocations = new AtomicInteger();
		FluxOperationInvoker target = new FluxOperationInvoker();
		InvocationContext context = new InvocationContext(mock(SecurityContext.class), Collections.emptyMap());
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, CACHE_TTL);
		Object response = ((Flux<?>) invoker.invoke(context)).blockLast();
		Object cachedResponse = ((Flux<?>) invoker.invoke(context)).blockLast();
		assertThat(FluxOperationInvoker.invocations).hasValue(1);
		assertThat(response).isSameAs(cachedResponse);
	}

	@Test // gh-28313
	void cacheWhenEachPrincipalIsUniqueDoesNotConsumeTooMuchMemory() throws Exception {
		MonoOperationInvoker target = new MonoOperationInvoker();
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, 50L);
		int count = 1000;
		for (int i = 0; i < count; i++) {
			invokeWithUniquePrincipal(invoker);
		}
		long expired = System.currentTimeMillis() + 50;
		while (System.currentTimeMillis() < expired) {
			Thread.sleep(10);
		}
		invokeWithUniquePrincipal(invoker);
		assertThat(invoker).extracting("cachedResponses", as(InstanceOfAssertFactories.MAP)).hasSizeLessThan(count);
	}

	private void invokeWithUniquePrincipal(CachingOperationInvoker invoker) {
		SecurityContext securityContext = mock(SecurityContext.class);
		Principal principal = mock(Principal.class);
		given(securityContext.getPrincipal()).willReturn(principal);
		InvocationContext context = new InvocationContext(securityContext, Collections.emptyMap());
		((Mono<?>) invoker.invoke(context)).block();
	}

	private void assertCacheIsUsed(Map<String, Object> parameters) {
		assertCacheIsUsed(parameters, null);
	}

	private void assertCacheIsUsed(Map<String, Object> parameters, Principal principal) {
		OperationInvoker target = mock(OperationInvoker.class);
		Object expected = new Object();
		SecurityContext securityContext = mock(SecurityContext.class);
		if (principal != null) {
			given(securityContext.getPrincipal()).willReturn(principal);
		}
		InvocationContext context = new InvocationContext(securityContext, parameters);
		given(target.invoke(context)).willReturn(expected);
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, CACHE_TTL);
		Object response = invoker.invoke(context);
		assertThat(response).isSameAs(expected);
		then(target).should().invoke(context);
		Object cachedResponse = invoker.invoke(context);
		assertThat(cachedResponse).isSameAs(response);
		then(target).shouldHaveNoMoreInteractions();
	}

	@Test
	void targetAlwaysInvokedWithParameters() {
		OperationInvoker target = mock(OperationInvoker.class);
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("test", "value");
		parameters.put("something", null);
		InvocationContext context = new InvocationContext(mock(SecurityContext.class), parameters);
		given(target.invoke(context)).willReturn(new Object());
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, CACHE_TTL);
		invoker.invoke(context);
		invoker.invoke(context);
		invoker.invoke(context);
		then(target).should(times(3)).invoke(context);
	}

	@Test
	void targetAlwaysInvokedWithDifferentPrincipals() {
		OperationInvoker target = mock(OperationInvoker.class);
		Map<String, Object> parameters = new HashMap<>();
		SecurityContext securityContext = mock(SecurityContext.class);
		given(securityContext.getPrincipal()).willReturn(mock(Principal.class), mock(Principal.class),
				mock(Principal.class));
		InvocationContext context = new InvocationContext(securityContext, parameters);
		Object result1 = new Object();
		Object result2 = new Object();
		Object result3 = new Object();
		given(target.invoke(context)).willReturn(result1, result2, result3);
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, CACHE_TTL);
		assertThat(invoker.invoke(context)).isEqualTo(result1);
		assertThat(invoker.invoke(context)).isEqualTo(result2);
		assertThat(invoker.invoke(context)).isEqualTo(result3);
		then(target).should(times(3)).invoke(context);
	}

	@Test
	void targetInvokedWhenCalledWithAndWithoutPrincipal() {
		OperationInvoker target = mock(OperationInvoker.class);
		Map<String, Object> parameters = new HashMap<>();
		SecurityContext anonymous = mock(SecurityContext.class);
		SecurityContext authenticated = mock(SecurityContext.class);
		given(authenticated.getPrincipal()).willReturn(mock(Principal.class));
		InvocationContext anonymousContext = new InvocationContext(anonymous, parameters);
		Object anonymousResult = new Object();
		given(target.invoke(anonymousContext)).willReturn(anonymousResult);
		InvocationContext authenticatedContext = new InvocationContext(authenticated, parameters);
		Object authenticatedResult = new Object();
		given(target.invoke(authenticatedContext)).willReturn(authenticatedResult);
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, CACHE_TTL);
		assertThat(invoker.invoke(anonymousContext)).isEqualTo(anonymousResult);
		assertThat(invoker.invoke(authenticatedContext)).isEqualTo(authenticatedResult);
		assertThat(invoker.invoke(anonymousContext)).isEqualTo(anonymousResult);
		assertThat(invoker.invoke(authenticatedContext)).isEqualTo(authenticatedResult);
		then(target).should().invoke(anonymousContext);
		then(target).should().invoke(authenticatedContext);
	}

	@Test
	void targetInvokedWhenCacheExpires() throws InterruptedException {
		OperationInvoker target = mock(OperationInvoker.class);
		Map<String, Object> parameters = new HashMap<>();
		InvocationContext context = new InvocationContext(mock(SecurityContext.class), parameters);
		given(target.invoke(context)).willReturn(new Object());
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, 50L);
		invoker.invoke(context);
		long expired = System.currentTimeMillis() + 50;
		while (System.currentTimeMillis() < expired) {
			Thread.sleep(10);
		}
		invoker.invoke(context);
		then(target).should(times(2)).invoke(context);
	}

	@Test
	void targetInvokedWithDifferentApiVersion() {
		OperationInvoker target = mock(OperationInvoker.class);
		Object expectedV2 = new Object();
		Object expectedV3 = new Object();
		InvocationContext contextV2 = new InvocationContext(mock(SecurityContext.class), Collections.emptyMap(),
				new ApiVersionArgumentResolver(ApiVersion.V2));
		InvocationContext contextV3 = new InvocationContext(mock(SecurityContext.class), Collections.emptyMap(),
				new ApiVersionArgumentResolver(ApiVersion.V3));
		given(target.invoke(contextV2)).willReturn(expectedV2);
		given(target.invoke(contextV3)).willReturn(expectedV3);
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, CACHE_TTL);
		Object responseV2 = invoker.invoke(contextV2);
		assertThat(responseV2).isSameAs(expectedV2);
		then(target).should().invoke(contextV2);
		Object responseV3 = invoker.invoke(contextV3);
		assertThat(responseV3).isNotSameAs(responseV2);
		then(target).should().invoke(contextV3);
	}

	@Test
	void targetInvokedWithDifferentWebServerNamespace() {
		OperationInvoker target = mock(OperationInvoker.class);
		Object expectedServer = new Object();
		Object expectedManagement = new Object();
		InvocationContext contextServer = new InvocationContext(mock(SecurityContext.class), Collections.emptyMap(),
				new WebServerNamespaceArgumentResolver(WebServerNamespace.SERVER));
		InvocationContext contextManagement = new InvocationContext(mock(SecurityContext.class), Collections.emptyMap(),
				new WebServerNamespaceArgumentResolver(WebServerNamespace.MANAGEMENT));
		given(target.invoke(contextServer)).willReturn(expectedServer);
		given(target.invoke(contextManagement)).willReturn(expectedManagement);
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, CACHE_TTL);
		Object responseServer = invoker.invoke(contextServer);
		assertThat(responseServer).isSameAs(expectedServer);
		then(target).should(times(1)).invoke(contextServer);
		Object responseManagement = invoker.invoke(contextManagement);
		assertThat(responseManagement).isNotSameAs(responseServer);
		then(target).should(times(1)).invoke(contextManagement);
	}

	private static class MonoOperationInvoker implements OperationInvoker {

		static AtomicInteger invocations = new AtomicInteger();

		@Override
		public Mono<String> invoke(InvocationContext context) {
			return Mono.fromCallable(() -> {
				invocations.incrementAndGet();
				return "test";
			});
		}

	}

	private static class FluxOperationInvoker implements OperationInvoker {

		static AtomicInteger invocations = new AtomicInteger();

		@Override
		public Flux<String> invoke(InvocationContext context) {
			return Flux.just("spring", "boot").hide().doFirst(invocations::incrementAndGet);
		}

	}

	private static final class ApiVersionArgumentResolver implements OperationArgumentResolver {

		private final ApiVersion apiVersion;

		private ApiVersionArgumentResolver(ApiVersion apiVersion) {
			this.apiVersion = apiVersion;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T resolve(Class<T> type) {
			return (T) this.apiVersion;
		}

		@Override
		public boolean canResolve(Class<?> type) {
			return ApiVersion.class.equals(type);
		}

	}

	private static final class WebServerNamespaceArgumentResolver implements OperationArgumentResolver {

		private final WebServerNamespace webServerNamespace;

		private WebServerNamespaceArgumentResolver(WebServerNamespace webServerNamespace) {
			this.webServerNamespace = webServerNamespace;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T resolve(Class<T> type) {
			return (T) this.webServerNamespace;
		}

		@Override
		public boolean canResolve(Class<?> type) {
			return WebServerNamespace.class.equals(type);
		}

	}

}
