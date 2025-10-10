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

package org.springframework.boot.maven;

import org.jspecify.annotations.Nullable;

/**
 * Format System properties.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
final class SystemPropertyFormatter {

	private SystemPropertyFormatter() {
	}

	static String format(@Nullable String key, @Nullable String value) {
		if (key == null) {
			return "";
		}
		if (value == null || value.isEmpty()) {
			return String.format("-D%s", key);
		}
		return String.format("-D%s=%s", key, value);
	}

}
