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

package org.springframework.boot.context.properties.bind;

import org.springframework.core.env.PropertyResolver;

/**
 * Optional strategy that used by a {@link Binder} to resolve property placeholders.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 * @see PropertySourcesPlaceholdersResolver
 */
@FunctionalInterface
public interface PlaceholdersResolver {

	/**
	 * No-op {@link PropertyResolver}.
	 */
	PlaceholdersResolver NONE = (value) -> value;

	/**
	 * Called to resolve any place holders in the given value.
	 * @param value the source value
	 * @return a value with place holders resolved
	 */
	Object resolvePlaceholders(Object value);

}
