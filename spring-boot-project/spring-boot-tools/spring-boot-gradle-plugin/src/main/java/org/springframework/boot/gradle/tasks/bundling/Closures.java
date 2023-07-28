/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.gradle.tasks.bundling;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.util.GradleVersion;

/**
 * Wrapper for {@code ConfigureUtil} that handles the API and behavior differences in the
 * versions of Gradle that we support.
 * <p>
 * In Gradle 7.1 {@code org.gradle.util.ConfigureUtil} was deprecated and
 * {@code org.gradle.util.internal.ConfigureUtil} was introduced as a replacement. In
 * Gradle 7.6, the deprecation of {@code org.gradle.util.ConfigureUtil} was strengthened
 * by reporting the use of deprecated API to the user.
 * <p>
 * To accommodate the differences, we use {@code org.gradle.util.internal.ConfigureUtil}
 * with Gradle 7.1 and later. With earlier versions, {@code org.gradle.util.ConfigureUtil}
 * is used. This avoids users being nagged about deprecated API usage in our plugin.
 *
 * @author Andy Wilkinson
 */
final class Closures {

	private Closures() {

	}

	@SuppressWarnings("deprecation")
	static <T> Action<T> asAction(Closure<?> closure) {
		if (GradleVersion.current().compareTo(GradleVersion.version("7.1")) < 0) {
			return org.gradle.util.ConfigureUtil.configureUsing(closure);
		}
		return org.gradle.util.internal.ConfigureUtil.configureUsing(closure);
	}

}
