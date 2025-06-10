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

package org.springframework.boot.actuate.endpoint.jackson;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;

/**
 * Interface used to supply the {@link ObjectMapper} that should be used when serializing
 * endpoint results.
 *
 * @author Phillip Webb
 * @since 3.0.0
 * @see OperationResponseBody
 */
public interface EndpointObjectMapper {

	/**
	 * The default supported types.
	 */
	Set<Class<?>> DEFAULT_SUPPORTED_TYPES = Set.of(OperationResponseBody.class);

	/**
	 * Return the {@link ObjectMapper} that should be used to serialize
	 * {@link OperationResponseBody} endpoint results.
	 * @return the object mapper
	 */
	ObjectMapper get();

	/**
	 * Return the types that this endpoint mapper supports.
	 * @return the supported types
	 * @since 4.0.0
	 */
	default Set<Class<?>> getSupportedTypes() {
		return DEFAULT_SUPPORTED_TYPES;
	}

}
