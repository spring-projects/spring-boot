/*
 * Copyright 2012-2017 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.InternalMockHandler;
import org.mockito.internal.matchers.LocalizedMatcher;
import org.mockito.internal.progress.ArgumentMatcherStorage;
import org.mockito.internal.progress.MockingProgress;
import org.mockito.internal.progress.ThreadSafeMockingProgress;
import org.mockito.internal.stubbing.InvocationContainer;
import org.mockito.internal.util.MockUtil;
import org.mockito.internal.verification.MockAwareVerificationMode;
import org.mockito.mock.MockCreationSettings;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;

import org.springframework.beans.BeanUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * A facade for Mockito APIs that have changed between Mockito 1 and Mockito 2.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
abstract class MockitoApi {

	private static final MockitoApi api = createApi();

	/**
	 * Return mock settings for the given mock object.
	 * @param mock the mock object
	 * @return the mock creation settings
	 */
	public abstract MockCreationSettings<?> getMockSettings(Object mock);

	/**
	 * Return the mocking progress for the current thread.
	 * @param mock the bean under test
	 * @return the current mocking progress
	 */
	public abstract MockingProgress mockingProgress(Object mock);

	/**
	 * Set report matchers to the given storage.
	 * @param storage the storage to use
	 * @param matchers the matchers to set
	 */
	public abstract void reportMatchers(ArgumentMatcherStorage storage,
			List<LocalizedMatcher> matchers);

	/**
	 * Create a new {@link MockAwareVerificationMode} instance.
	 * @param mock the source mock
	 * @param mode the verification mode
	 * @return a new {@link MockAwareVerificationMode} instance
	 */
	public abstract MockAwareVerificationMode createMockAwareVerificationMode(Object mock,
			VerificationMode mode);

	/**
	 * Return the {@link Answer} for a given {@link Answers} value.
	 * @param answer the source answers
	 * @return the answer
	 */
	public abstract Answer<Object> getAnswer(Answers answer);

	/**
	 * Factory to create the appropriate API version.
	 * @return the API version
	 */
	private static MockitoApi createApi() {
		if (ClassUtils.isPresent("org.mockito.quality.MockitoHint", null)) {
			return new Mockito2Api();
		}
		return new Mockito1Api();
	}

	/**
	 * Get the API for the running mockito version.
	 * @return the API
	 */
	public static MockitoApi get() {
		return api;
	}

	/**
	 * {@link MockitoApi} for Mockito 1.0.
	 */
	private static class Mockito1Api extends MockitoApi {

		@Override
		public MockCreationSettings<?> getMockSettings(Object mock) {
			return new MockUtil().getMockSettings(mock);
		}

		@Override
		public MockingProgress mockingProgress(Object mock) {
			MockUtil mockUtil = new MockUtil();
			InternalMockHandler<?> handler = mockUtil.getMockHandler(mock);
			InvocationContainer container = handler.getInvocationContainer();
			Field field = ReflectionUtils.findField(container.getClass(),
					"mockingProgress");
			ReflectionUtils.makeAccessible(field);
			return (MockingProgress) ReflectionUtils.getField(field, container);
		}

		@Override
		public void reportMatchers(ArgumentMatcherStorage storage,
				List<LocalizedMatcher> matchers) {
			for (LocalizedMatcher matcher : matchers) {
				storage.reportMatcher(matcher);
			}
		}

		@Override
		public MockAwareVerificationMode createMockAwareVerificationMode(Object mock,
				VerificationMode mode) {
			return new MockAwareVerificationMode(mock, mode);
		}

		@Override
		public Answer<Object> getAnswer(Answers answer) {
			return answer.get();
		}

	}

	/**
	 * {@link MockitoApi} for Mockito 2.0.
	 */
	private static class Mockito2Api extends MockitoApi {

		private final Method getMockSettingsMethod;

		private final Method mockingProgressMethod;

		private final Method reportMatcherMethod;

		private final Method getMatcherMethod;

		private final Constructor<MockAwareVerificationMode> mockAwareVerificationModeConstructor;

		Mockito2Api() {
			this.getMockSettingsMethod = ReflectionUtils.findMethod(MockUtil.class,
					"getMockSettings", Object.class);
			this.mockingProgressMethod = ReflectionUtils
					.findMethod(ThreadSafeMockingProgress.class, "mockingProgress");
			this.reportMatcherMethod = ReflectionUtils.findMethod(
					ArgumentMatcherStorage.class, "reportMatcher", ArgumentMatcher.class);
			this.getMatcherMethod = ReflectionUtils.findMethod(LocalizedMatcher.class,
					"getMatcher");
			this.mockAwareVerificationModeConstructor = ClassUtils
					.getConstructorIfAvailable(MockAwareVerificationMode.class,
							Object.class, VerificationMode.class, Set.class);
		}

		@Override
		public MockCreationSettings<?> getMockSettings(Object mock) {
			return (MockCreationSettings<?>) ReflectionUtils
					.invokeMethod(this.getMockSettingsMethod, null, mock);
		}

		@Override
		public MockingProgress mockingProgress(Object mock) {
			return (MockingProgress) ReflectionUtils
					.invokeMethod(this.mockingProgressMethod, null);
		}

		@Override
		public void reportMatchers(ArgumentMatcherStorage storage,
				List<LocalizedMatcher> matchers) {
			for (LocalizedMatcher matcher : matchers) {
				ReflectionUtils.invokeMethod(this.reportMatcherMethod, storage,
						ReflectionUtils.invokeMethod(this.getMatcherMethod, matcher));
			}
		}

		@Override
		public MockAwareVerificationMode createMockAwareVerificationMode(Object mock,
				VerificationMode mode) {
			if (this.mockAwareVerificationModeConstructor != null) {
				// Later 2.0 releases include a listener set
				return BeanUtils.instantiateClass(
						this.mockAwareVerificationModeConstructor, mock, mode,
						Collections.emptySet());
			}
			return new MockAwareVerificationMode(mock, mode);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Answer<Object> getAnswer(Answers answer) {
			return (Answer<Object>) ((Object) answer);
		}

	}

}
