/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matcher;
import org.mockito.Answers;
import org.mockito.internal.matchers.LocalizedMatcher;
import org.mockito.internal.progress.ArgumentMatcherStorage;
import org.mockito.internal.progress.MockingProgress;
import org.mockito.internal.progress.ThreadSafeMockingProgress;
import org.mockito.internal.util.MockUtil;
import org.mockito.internal.verification.MockAwareVerificationMode;
import org.mockito.mock.MockCreationSettings;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;
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
	 * @return the current mocking progress
	 */
	public abstract MockingProgress mockingProgress();

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
		if (!ClassUtils.isPresent("org.mockito.ReturnValues",
				MockitoApi.class.getClassLoader())) {
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
	 * {@link MockitoApi} for Mockito 2.0.
	 */
	private static class Mockito2Api extends MockitoApi {

		@Override
		public MockCreationSettings<?> getMockSettings(Object mock) {
			return MockUtil.getMockSettings(mock);
		}

		@Override
		public MockingProgress mockingProgress() {
			return ThreadSafeMockingProgress.mockingProgress();
		}

		@Override
		public void reportMatchers(ArgumentMatcherStorage storage,
				List<LocalizedMatcher> matchers) {
			for (LocalizedMatcher matcher : matchers) {
				storage.reportMatcher(matcher.getMatcher());
			}
		}

		@Override
		public MockAwareVerificationMode createMockAwareVerificationMode(Object mock,
				VerificationMode mode) {
			try {
				return new MockAwareVerificationMode(mock, mode, Collections.emptySet());
			}
			catch (NoSuchMethodError ex) {
				// Earlier versions of 2.x did not have the collection parameter
				Constructor<MockAwareVerificationMode> constructor = ClassUtils
						.getConstructorIfAvailable(MockAwareVerificationMode.class,
								Object.class, VerificationMode.class);
				if (constructor == null) {
					throw ex;
				}
				return BeanUtils.instantiateClass(constructor, mock, mode);
			}
		}

		@Override
		public Answer<Object> getAnswer(Answers answer) {
			return answer;
		}

	}

	/**
	 * {@link MockitoApi} for Mockito 1.0.
	 */
	private static class Mockito1Api extends MockitoApi {

		private final MockUtil mockUtil;

		private final Method getMockSettingsMethod;

		private final MockingProgress mockingProgress;

		private Method reportMatcherMethod;

		private Constructor<MockAwareVerificationMode> mockAwareVerificationModeConstructor;

		Mockito1Api() {
			this.mockUtil = BeanUtils.instantiateClass(MockUtil.class);
			this.getMockSettingsMethod = findMockSettingsMethod();
			this.mockingProgress = (MockingProgress) BeanUtils
					.instantiateClass(ClassUtils.resolveClassName(
							"org.mockito.internal.progress.ThreadSafeMockingProgress",
							MockitoApi.class.getClassLoader()));
			this.reportMatcherMethod = findReportMatcherMethod();
			this.mockAwareVerificationModeConstructor = findMockAwareVerificationModeConstructor();
		}

		private Method findMockSettingsMethod() {
			Method method = ReflectionUtils.findMethod(MockUtil.class, "getMockSettings",
					Object.class);
			Assert.state(method != null, "Unable to find getMockSettings method");
			return method;
		}

		private Method findReportMatcherMethod() {
			Method method = ReflectionUtils.findMethod(ArgumentMatcherStorage.class,
					"reportMatcher", Matcher.class);
			Assert.state(method != null, "Unable to find reportMatcher method");
			return method;
		}

		private Constructor<MockAwareVerificationMode> findMockAwareVerificationModeConstructor() {
			Constructor<MockAwareVerificationMode> constructor = ClassUtils
					.getConstructorIfAvailable(MockAwareVerificationMode.class,
							Object.class, VerificationMode.class);
			Assert.state(constructor != null,
					"Unable to find MockAwareVerificationMode constructor");
			return constructor;
		}

		@Override
		public MockCreationSettings<?> getMockSettings(Object mock) {
			return (MockCreationSettings<?>) ReflectionUtils
					.invokeMethod(this.getMockSettingsMethod, this.mockUtil, mock);
		}

		@Override
		public MockingProgress mockingProgress() {
			return this.mockingProgress;
		}

		@Override
		public void reportMatchers(ArgumentMatcherStorage storage,
				List<LocalizedMatcher> matchers) {
			for (LocalizedMatcher matcher : matchers) {
				ReflectionUtils.invokeMethod(this.reportMatcherMethod, storage, matcher);
			}
		}

		@Override
		public MockAwareVerificationMode createMockAwareVerificationMode(Object mock,
				VerificationMode mode) {
			return BeanUtils.instantiateClass(this.mockAwareVerificationModeConstructor,
					mock, mode);
		}

		@Override
		@SuppressWarnings("deprecation")
		public Answer<Object> getAnswer(Answers answer) {
			return answer.get();
		}

	}

}
