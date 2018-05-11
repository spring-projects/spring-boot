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

package org.springframework.boot;

import java.lang.reflect.InvocationTargetException;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;


import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link SpringBootExceptionHandler}.
 *
 * @author Henri Tremblay
 */
public class SpringBootExceptionHandlerTest {

	@Rule
	public MockitoRule rule = MockitoJUnit.rule();

	@Mock
	private Thread.UncaughtExceptionHandler parent;

	@InjectMocks
	private SpringBootExceptionHandler handler;

	@Test
	public void uncaughtException_shouldNotForwardLoggedErrorToParent() {
		Thread thread = Thread.currentThread();
		Exception ex = new Exception();
		this.handler.registerLoggedException(ex);

		this.handler.uncaughtException(thread, ex);

		verifyZeroInteractions(this.parent);
	}

	@Test
	public void uncaughtException_shouldForwardLogConfigurationErrorToParent() {
		Thread thread = Thread.currentThread();
		Exception ex = new Exception("[stuff] Logback configuration error detected [stuff]");
		this.handler.registerLoggedException(ex);

		this.handler.uncaughtException(thread, ex);

		verify(this.parent).uncaughtException(same(thread), same(ex));
	}

	@Test
	public void uncaughtException_shouldForwardLogConfigurationErrorToParentEvenWhenWrapped() {
		Thread thread = Thread.currentThread();
		Exception ex = new InvocationTargetException(new Exception("[stuff] Logback configuration error detected [stuff]", new Exception()));
		this.handler.registerLoggedException(ex);

		this.handler.uncaughtException(thread, ex);

		verify(this.parent).uncaughtException(same(thread), same(ex));
	}
}
