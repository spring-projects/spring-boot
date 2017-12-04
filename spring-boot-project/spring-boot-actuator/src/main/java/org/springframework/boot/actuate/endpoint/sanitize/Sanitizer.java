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

package org.springframework.boot.actuate.endpoint.sanitize;

import java.util.Collection;
import java.util.Collections;

/**
 * Strategy that should be used by endpoint implementations to sanitize potentially
 * sensitive keys.
 *
 * @param <T> the key type
 * @author Christian Dupuis
 * @author Toshiaki Maki
 * @author Phillip Webb
 * @author Nicolas Lejeune
 * @author Stephane Nicoll
 * @author Piotr Betkier
 * @since 2.0.0
 */
public final class Sanitizer<T> {

	private final Collection<Filter<T>> filters;

	private Sanitizer(Collection<Filter<T>> filters) {
		this.filters = filters;
	}

	/**
	 * Creates a sanitizer that sanitizes keys matching the default blacklist.
	 * @return a new sanitizer instance
	 */
	public static Sanitizer<String> keyBlacklistSanitizer() {
		return new Sanitizer<>(
				Collections.singleton(KeyBlacklistSanitizeFilter.matchingDefaultKeys()));
	}

	/**
	 * Creates a sanitizer that sanitizes keys matching the given blacklist.
	 * @param keysToSanitize the keys to match
	 * @return a new sanitizer instance
	 */
	public static Sanitizer<String> keyBlacklistSanitizer(String... keysToSanitize) {
		return new Sanitizer<>(Collections
				.singleton(KeyBlacklistSanitizeFilter.matchingKeys(keysToSanitize)));
	}

	/**
	 * Creates a sanitizer that sanitizes keys matched by any of the given filters.
	 * @param filters the filters to use
	 * @param <T> the key type
	 * @return a new sanitizer instance
	 */
	public static <T> Sanitizer<T> customRulesSanitizer(Collection<Filter<T>> filters) {
		return new Sanitizer<>(filters);
	}

	/**
	 * Sanitize the given value if necessary.
	 * @param key the value key, e.g. property name
	 * @param value the value
	 * @return the potentially sanitized value
	 */
	public Object sanitize(T key, Object value) {
		if (value == null) {
			return null;
		}

		if (this.filters.stream().anyMatch(f -> f.match(key))) {
			return "******";
		}

		return value;
	}

	/**
	 * Should the value for the given key be sanitized.
	 * @param <T> the key type
	 */
	public interface Filter<T> {
		boolean match(T key);
	}

}
