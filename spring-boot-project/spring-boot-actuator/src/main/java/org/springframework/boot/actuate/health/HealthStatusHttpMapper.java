/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.util.Assert;

/**
 * Map a {@link Status} to an HTTP status code.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 * @deprecated since 2.2.0 in favor of {@link HttpCodeStatusMapper} or
 * {@link SimpleHttpCodeStatusMapper}
 */
@Deprecated
public class HealthStatusHttpMapper {

	private Map<String, Integer> statusMapping = new HashMap<>();

	/**
	 * Create a new instance.
	 */
	public HealthStatusHttpMapper() {
		setupDefaultStatusMapping();
	}

	private void setupDefaultStatusMapping() {
		addStatusMapping(Status.DOWN, WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);
		addStatusMapping(Status.OUT_OF_SERVICE, WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);
	}

	/**
	 * Set specific status mappings.
	 * @param statusMapping a map of health status code to HTTP status code
	 */
	public void setStatusMapping(Map<String, Integer> statusMapping) {
		Assert.notNull(statusMapping, "StatusMapping must not be null");
		this.statusMapping = new HashMap<>(statusMapping);
	}

	/**
	 * Add specific status mappings to the existing set.
	 * @param statusMapping a map of health status code to HTTP status code
	 */
	public void addStatusMapping(Map<String, Integer> statusMapping) {
		Assert.notNull(statusMapping, "StatusMapping must not be null");
		this.statusMapping.putAll(statusMapping);
	}

	/**
	 * Add a status mapping to the existing set.
	 * @param status the status to map
	 * @param httpStatus the http status
	 */
	public void addStatusMapping(Status status, Integer httpStatus) {
		Assert.notNull(status, "Status must not be null");
		Assert.notNull(httpStatus, "HttpStatus must not be null");
		addStatusMapping(status.getCode(), httpStatus);
	}

	/**
	 * Add a status mapping to the existing set.
	 * @param statusCode the status code to map
	 * @param httpStatus the http status
	 */
	public void addStatusMapping(String statusCode, Integer httpStatus) {
		Assert.notNull(statusCode, "StatusCode must not be null");
		Assert.notNull(httpStatus, "HttpStatus must not be null");
		this.statusMapping.put(statusCode, httpStatus);
	}

	/**
	 * Return an immutable view of the status mapping.
	 * @return the http status codes mapped by status name
	 */
	public Map<String, Integer> getStatusMapping() {
		return Collections.unmodifiableMap(this.statusMapping);
	}

	/**
	 * Map the specified {@link Status} to an HTTP status code.
	 * @param status the health {@link Status}
	 * @return the corresponding HTTP status code
	 */
	public int mapStatus(Status status) {
		String code = getUniformValue(status.getCode());
		if (code != null) {
			return this.statusMapping.entrySet().stream()
					.filter((entry) -> code.equals(getUniformValue(entry.getKey()))).map(Map.Entry::getValue)
					.findFirst().orElse(WebEndpointResponse.STATUS_OK);
		}
		return WebEndpointResponse.STATUS_OK;
	}

	private String getUniformValue(String code) {
		if (code == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for (char ch : code.toCharArray()) {
			if (Character.isAlphabetic(ch) || Character.isDigit(ch)) {
				builder.append(Character.toLowerCase(ch));
			}
		}
		return builder.toString();
	}

}
