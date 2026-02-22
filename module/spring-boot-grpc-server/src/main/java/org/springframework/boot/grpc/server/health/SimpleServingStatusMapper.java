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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.Status;
import org.springframework.lang.Contract;

/**
 * Simple {@link StatusMapper} backed by map of {@link Status#getCode() status code} to
 * {@link ServingStatus}.
 *
 * @author Phillip Webb
 */
class SimpleServingStatusMapper implements StatusMapper {

	static final SimpleServingStatusMapper DEFAULT_MAPPINGS;
	static {
		Map<String, ServingStatus> mappings = new HashMap<>();
		mappings.put(Status.DOWN.getCode(), ServingStatus.NOT_SERVING);
		mappings.put(Status.OUT_OF_SERVICE.getCode(), ServingStatus.NOT_SERVING);
		mappings.put(Status.UNKNOWN.getCode(), ServingStatus.UNKNOWN);
		DEFAULT_MAPPINGS = new SimpleServingStatusMapper(mappings);
	}

	private final Map<String, ServingStatus> mappings;

	SimpleServingStatusMapper(Map<String, ServingStatus> mappings) {
		this.mappings = getUniformMappings(mappings);
	}

	@Override
	public ServingStatus getServingStatus(Status status) {
		String code = getUniformCode(status.getCode());
		return this.mappings.getOrDefault(code, ServingStatus.SERVING);
	}

	private static Map<String, ServingStatus> getUniformMappings(Map<String, ServingStatus> mappings) {
		Map<String, ServingStatus> result = new LinkedHashMap<>();
		for (Map.Entry<String, ServingStatus> entry : mappings.entrySet()) {
			String code = getUniformCode(entry.getKey());
			if (code != null) {
				result.putIfAbsent(code, entry.getValue());
			}
		}
		return Collections.unmodifiableMap(result);
	}

	@Contract("!null -> !null")
	private static @Nullable String getUniformCode(@Nullable String code) {
		return (code != null) ? code.codePoints()
			.filter(Character::isLetterOrDigit)
			.map(Character::toLowerCase)
			.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
			.toString() : null;
	}

}
