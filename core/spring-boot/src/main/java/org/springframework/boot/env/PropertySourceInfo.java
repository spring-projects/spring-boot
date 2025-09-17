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

package org.springframework.boot.env;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.PropertySource;

/**
 * Interface that can be optionally implemented by a {@link PropertySource} to provide
 * additional information.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface PropertySourceInfo {

	/**
	 * Return {@code true} if this lookup is immutable and has contents that will never
	 * change.
	 * @return if the lookup is immutable
	 */
	default boolean isImmutable() {
		return false;
	}

	/**
	 * Return the implicit prefix that is applied when performing a lookup or {@code null}
	 * if no prefix is used. Prefixes can be used to disambiguate keys that would
	 * otherwise clash. For example, if multiple applications are running on the same
	 * machine a different prefix can be set on each application to ensure that different
	 * environment variables are used.
	 * @return the prefix applied by the lookup class or {@code null}.
	 */
	default @Nullable String getPrefix() {
		return null;
	}

}
