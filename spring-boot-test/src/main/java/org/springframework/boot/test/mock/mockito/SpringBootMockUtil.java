/*
 * Copyright 2012-2017 the original author or authors.
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
import java.util.Set;

import org.hamcrest.Matcher;
import org.mockito.internal.matchers.LocalizedMatcher;
import org.mockito.internal.progress.ArgumentMatcherStorage;
import org.mockito.internal.progress.MockingProgress;
import org.mockito.internal.progress.ThreadSafeMockingProgress;
import org.mockito.internal.util.MockUtil;
import org.mockito.internal.verification.MockAwareVerificationMode;
import org.mockito.mock.MockCreationSettings;
import org.mockito.verification.VerificationMode;

import org.springframework.beans.BeanUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * A facade for Mockito's {@link MockUtil} that hides API differences between Mockito 1
 * and 2.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
final class SpringBootMockUtil {

	private static final MockUtilAdapter adapter;

	static {
		if (ClassUtils.isPresent("org.mockito.ReturnValues",
				SpringBootMockUtil.class.getClassLoader())) {
			adapter = new Mockito1MockUtilAdapter();
		}
		else {
			adapter = new Mockito2MockUtilAdapter();
		}
	}

	private SpringBootMockUtil() {

	}

	static MockCreationSettings<?> getMockSettings(Object mock) {
		return adapter.getMockSettings(mock);
	}

	static MockingProgress mockingProgress() {
		return adapter.mockingProgress();
	}

	static void reportMatchers(ArgumentMatcherStorage storage,
			List<LocalizedMatcher> matchers) {
		adapter.reportMatchers(storage, matchers);
	}

	static MockAwareVerificationMode createMockAwareVerificationMode(Object mock,
			VerificationMode mode) {
		return adapter.createMockAwareVerificationMode(mock, mode);
	}

	private interface MockUtilAdapter {

		MockCreationSettings<?> getMockSettings(Object mock);

		MockingProgress mockingProgress();

		void reportMatchers(ArgumentMatcherStorage storage,
				List<LocalizedMatcher> matchers);

		MockAwareVerificationMode createMockAwareVerificationMode(Object mock,
				VerificationMode mode);

	}

	private static class Mockito1MockUtilAdapter implements MockUtilAdapter {

		private final MockUtil mockUtil = BeanUtils.instantiate(MockUtil.class);

		private final Method getMockSettingsMethod = ReflectionUtils
				.findMethod(MockUtil.class, "getMockSettings", Object.class);

		private static final MockingProgress mockingProgress =
				createThreadSafeMockingProgress();

		private final Method reportMatcherMethod = ReflectionUtils.findMethod(
				ArgumentMatcherStorage.class, "reportMatcher", Matcher.class);

		private static final Constructor<MockAwareVerificationMode> mockAwareVerificationModeConstructor =
				getMockAwareVerificationModeConstructor();

		private static MockingProgress createThreadSafeMockingProgress() {
			try {
				Class<?> target = ClassUtils.forName(
						"org.mockito.internal.progress.ThreadSafeMockingProgress",
						SpringBootMockUtil.class.getClassLoader());
				return (MockingProgress) BeanUtils.instantiateClass(target);
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private static Constructor<MockAwareVerificationMode> getMockAwareVerificationModeConstructor() {
			try {
				return MockAwareVerificationMode.class.getConstructor(Object.class,
						VerificationMode.class);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException(ex);
			}
		}

		@Override
		public MockCreationSettings<?> getMockSettings(Object mock) {
			return (MockCreationSettings<?>) ReflectionUtils
					.invokeMethod(this.getMockSettingsMethod, this.mockUtil, mock);
		}

		@Override
		public MockingProgress mockingProgress() {
			return mockingProgress;
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
			return BeanUtils.instantiateClass(mockAwareVerificationModeConstructor, mock,
					mode);
		}
	}

	private static class Mockito2MockUtilAdapter implements MockUtilAdapter {

		private static final Constructor<MockAwareVerificationMode> mockAwareVerificationModeConstructor;
		private static final boolean mockAwareVerificationModeLegacy;

		static {
			Constructor<MockAwareVerificationMode> c = getMockAwareVerificationModeConstructor();
			if (c != null) {
				mockAwareVerificationModeConstructor = c;
				mockAwareVerificationModeLegacy = false;
			}
			else {
				mockAwareVerificationModeConstructor = getMockAwareVerificationModeLegacyConstructor();
				mockAwareVerificationModeLegacy = true;
			}
		}

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
			if (mockAwareVerificationModeLegacy) {
				return BeanUtils.instantiateClass(mockAwareVerificationModeConstructor,
						mock, mode);
			}
			else {
				return BeanUtils.instantiateClass(mockAwareVerificationModeConstructor,
						mock, mode, Collections.emptySet());
			}
		}

		private static Constructor<MockAwareVerificationMode> getMockAwareVerificationModeLegacyConstructor() {
			try {
				return MockAwareVerificationMode.class.getConstructor(Object.class,
						VerificationMode.class);
			}
			catch (NoSuchMethodException ex) {
				return null;
			}
		}

		private static Constructor<MockAwareVerificationMode> getMockAwareVerificationModeConstructor() {
			try {
				return MockAwareVerificationMode.class.getConstructor(Object.class,
						VerificationMode.class, Set.class);
			}
			catch (NoSuchMethodException ex) {
				return null;
			}
		}

	}

}
