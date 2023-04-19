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

package org.springframework.boot.actuate.endpoint;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tagging interface used to indicate that an operation result is intended to be returned
 * in the body of the response. Primarily intended to support JSON serialization using an
 * endpoint specific {@link ObjectMapper}.
 *
 * @author Phillip Webb
 * @since 3.0.0
 */
public interface OperationResponseBody {

	/**
	 * Return a {@link OperationResponseBody} {@link Map} instance containing entries from
	 * the given {@code map}.
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param map the source map or {@code null}
	 * @return a {@link OperationResponseBody} version of the map or {@code null}
	 */
	static <K, V> Map<K, V> of(Map<K, V> map) {
		return (map != null) ? new OperationResponseBodyMap<>(map) : null;
	}

}
