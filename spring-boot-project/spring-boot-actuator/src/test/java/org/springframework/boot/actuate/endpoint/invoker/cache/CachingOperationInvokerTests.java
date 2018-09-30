/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint.invoker.cache;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link CachingOperationInvoker}.
 *
 * @author Stephane Nicoll
 */
public class CachingOperationInvokerTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void createInstanceWithTtlSetToZero() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TimeToLive");
		new CachingOperationInvoker(mock(OperationInvoker.class), 0);
	}

	@Test
	public void cacheInTtlRangeWithNoParameter() {
		assertCacheIsUsed(Collections.emptyMap());
	}

	@Test
	public void cacheInTtlWithNullParameters() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("first", null);
		parameters.put("second", null);
		assertCacheIsUsed(parameters);
	}

	private void assertCacheIsUsed(Map<String, Object> parameters) {
		OperationInvoker target = mock(OperationInvoker.class);
		Object expected = new Object();
		InvocationContext context = new InvocationContext(mock(SecurityContext.class),
				parameters);
		given(target.invoke(context)).willReturn(expected);
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, 500L);
		Object response = invoker.invoke(context);
		assertThat(response).isSameAs(expected);
		verify(target, times(1)).invoke(context);
		Object cachedResponse = invoker.invoke(context);
		assertThat(cachedResponse).isSameAs(response);
		verifyNoMoreInteractions(target);
	}

	@Test
	public void targetAlwaysInvokedWithParameters() {
		OperationInvoker target = mock(OperationInvoker.class);
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("test", "value");
		parameters.put("something", null);
		InvocationContext context = new InvocationContext(mock(SecurityContext.class),
				parameters);
		given(target.invoke(context)).willReturn(new Object());
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, 500L);
		invoker.invoke(context);
		invoker.invoke(context);
		invoker.invoke(context);
		verify(target, times(3)).invoke(context);
	}

	@Test
	public void targetAlwaysInvokedWithPrincipal() {
		OperationInvoker target = mock(OperationInvoker.class);
		Map<String, Object> parameters = new HashMap<>();
		SecurityContext securityContext = mock(SecurityContext.class);
		given(securityContext.getPrincipal()).willReturn(mock(Principal.class));
		InvocationContext context = new InvocationContext(securityContext, parameters);
		given(target.invoke(context)).willReturn(new Object());
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, 500L);
		invoker.invoke(context);
		invoker.invoke(context);
		invoker.invoke(context);
		verify(target, times(3)).invoke(context);
	}

	@Test
	public void targetInvokedWhenCacheExpires() throws InterruptedException {
		OperationInvoker target = mock(OperationInvoker.class);
		Map<String, Object> parameters = new HashMap<>();
		InvocationContext context = new InvocationContext(mock(SecurityContext.class),
				parameters);
		given(target.invoke(context)).willReturn(new Object());
		CachingOperationInvoker invoker = new CachingOperationInvoker(target, 50L);
		invoker.invoke(context);
		Thread.sleep(55);
		invoker.invoke(context);
		verify(target, times(2)).invoke(context);
	}

}
