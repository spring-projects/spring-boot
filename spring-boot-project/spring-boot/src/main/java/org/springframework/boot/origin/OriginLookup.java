/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.origin;

/**
 * An interface that may be implemented by an object that can lookup {@link Origin}
 * information from a given key. Can be used to add origin support to existing classes.
 *
 * @param <K> the lookup key type
 * @author Phillip Webb
 * @since 2.0.0
 */
@FunctionalInterface
public interface OriginLookup<K> {

	/**
	 * Return the origin of the given key or {@code null} if the origin cannot be
	 * determined.
	 * @param key the key to lookup
	 * @return the origin of the key or {@code null}
	 */
	Origin getOrigin(K key);

	/**
	 * Return {@code true} if this lookup is immutable and has contents that will never
	 * change.
	 * @return if the lookup is immutable
	 * @since 2.2.0
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
	 * @since 2.5.0
	 */
	default String getPrefix() {
		return null;
	}

	/**
	 * Attempt to lookup the origin from the given source. If the source is not a
	 * {@link OriginLookup} or if an exception occurs during lookup then {@code null} is
	 * returned.
	 * @param source the source object
	 * @param key the key to lookup
	 * @param <K> the key type
	 * @return an {@link Origin} or {@code null}
	 */
	@SuppressWarnings("unchecked")
	static <K> Origin getOrigin(Object source, K key) {
		if (!(source instanceof OriginLookup)) {
			return null;
		}
		try {
			return ((OriginLookup<K>) source).getOrigin(key);
		}
		catch (Throwable ex) {
			return null;
		}
	}

}
