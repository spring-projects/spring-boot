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

package org.springframework.boot.grpc.server.health;

import java.util.Map;

import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.Status;
import org.springframework.util.CollectionUtils;

/**
 * Strategy used to map a {@link Status health status} to a gRPC {@link ServingStatus}.
 *
 * @author Phillip Webb
 * @since 4.1.0
 */
@FunctionalInterface
public interface StatusMapper {

	/**
	 * Return the HTTP status code that corresponds to the given {@link Status health
	 * status}.
	 * @param status the health status to map
	 * @return the corresponding HTTP status code
	 */
	ServingStatus getServingStatus(Status status);

	/**
	 * Create a new {@link StatusMapper} with the specified mappings.
	 * @param mappings the mappings to use or {@code null} to use the default mappings
	 * @return a {@link StatusMapper} or {@link #getDefault()}
	 */
	static StatusMapper of(@Nullable Map<String, ServingStatus> mappings) {
		return CollectionUtils.isEmpty(mappings) ? SimpleServingStatusMapper.DEFAULT_MAPPINGS
				: new SimpleServingStatusMapper(mappings);
	}

	/**
	 * Return an {@link StatusMapper} instance using default mappings.
	 * @return a mapper using default mappings
	 */
	static StatusMapper getDefault() {
		return SimpleServingStatusMapper.DEFAULT_MAPPINGS;
	}

}
