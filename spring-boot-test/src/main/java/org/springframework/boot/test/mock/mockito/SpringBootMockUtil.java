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

import java.lang.reflect.Method;
import java.util.List;

import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.LocalizedMatcher;
import org.mockito.internal.progress.ArgumentMatcherStorage;
import org.mockito.internal.progress.MockingProgress;
import org.mockito.internal.progress.ThreadSafeMockingProgress;
import org.mockito.internal.util.MockUtil;
import org.mockito.mock.MockCreationSettings;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * A facade for Mockito's {@link MockUtil} that hides API differences between Mockito 1
 * and 2.
 *
 * @author Andy Wilkinson
 */
final class SpringBootMockUtil {

	private static final MockUtilAdapter adapter;

	static {
		if (ClassUtils.isPresent("org.mockito.quality.MockitoHint",
				SpringBootMockUtil.class.getClassLoader())) {
			adapter = new Mockito2MockUtilAdapter();
		}
		else {
			adapter = new Mockito1MockUtilAdapter();
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

	private interface MockUtilAdapter {

		MockCreationSettings<?> getMockSettings(Object mock);

		MockingProgress mockingProgress();

		void reportMatchers(ArgumentMatcherStorage storage,
				List<LocalizedMatcher> matchers);

	}

	private static class Mockito1MockUtilAdapter implements MockUtilAdapter {

		private static final MockingProgress mockingProgress = new ThreadSafeMockingProgress();

		@Override
		public MockCreationSettings<?> getMockSettings(Object mock) {
			return new MockUtil().getMockSettings(mock);
		}

		@Override
		public MockingProgress mockingProgress() {
			return mockingProgress;
		}

		@Override
		public void reportMatchers(ArgumentMatcherStorage storage,
				List<LocalizedMatcher> matchers) {
			for (LocalizedMatcher matcher : matchers) {
				storage.reportMatcher(matcher);
			}
		}

	}

	private static class Mockito2MockUtilAdapter implements MockUtilAdapter {

		private final Method getMockSettingsMethod = ReflectionUtils
				.findMethod(MockUtil.class, "getMockSettings", Object.class);

		private final Method mockingProgressMethod = ReflectionUtils
				.findMethod(ThreadSafeMockingProgress.class, "mockingProgress");

		private final Method reportMatcherMethod = ReflectionUtils.findMethod(
				ArgumentMatcherStorage.class, "reportMatcher", ArgumentMatcher.class);

		private final Method getMatcherMethod = ReflectionUtils
				.findMethod(LocalizedMatcher.class, "getMatcher");

		@Override
		public MockCreationSettings<?> getMockSettings(Object mock) {
			return (MockCreationSettings<?>) ReflectionUtils
					.invokeMethod(this.getMockSettingsMethod, null, mock);
		}

		@Override
		public MockingProgress mockingProgress() {
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

	}

}
