/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Adapter to expose {@link HealthEndpoint} as an {@link MvcEndpoint}.
 * 
 * @author Christian Dupuis
 * @since 1.1.0
 */
public class HealthMvcEndpoint extends EndpointMvcAdapter {

	private Map<String, HttpStatus> statusMapping = new HashMap<String, HttpStatus>();

	public HealthMvcEndpoint(HealthEndpoint delegate) {
		super(delegate);
		setupDefaultStatusMapping();
	}

	private void setupDefaultStatusMapping() {
		addStatusMapping(Status.DOWN, HttpStatus.SERVICE_UNAVAILABLE);
		addStatusMapping(Status.OUT_OF_SERVICE, HttpStatus.SERVICE_UNAVAILABLE);
	}

	/**
	 * Set specific status mappings.
	 * @param statusMapping a map of status code to {@link HttpStatus}
	 */
	public void setStatusMapping(Map<String, HttpStatus> statusMapping) {
		Assert.notNull(statusMapping, "StatusMapping must not be null");
		this.statusMapping = new HashMap<String, HttpStatus>(statusMapping);
	}

	/**
	 * Add specfic status mappings to the existing set.
	 * @param statusMapping a map of status code to {@link HttpStatus}
	 */
	public void addStatusMapping(Map<String, HttpStatus> statusMapping) {
		Assert.notNull(statusMapping, "StatusMapping must not be null");
		this.statusMapping.putAll(statusMapping);
	}

	/**
	 * Add a status mapping to the existing set.
	 * @param status the status to map
	 * @param httpStatus the http status
	 */
	public void addStatusMapping(Status status, HttpStatus httpStatus) {
		Assert.notNull(status, "Status must not be null");
		Assert.notNull(httpStatus, "HttpStatus must not be null");
		addStatusMapping(status.getCode(), httpStatus);
	}

	/**
	 * Add a status mapping to the existing set.
	 * @param statusCode the status code to map
	 * @param httpStatus the http status
	 */
	public void addStatusMapping(String statusCode, HttpStatus httpStatus) {
		Assert.notNull(statusCode, "StatusCode must not be null");
		Assert.notNull(httpStatus, "HttpStatus must not be null");
		this.statusMapping.put(statusCode, httpStatus);
	}

	@RequestMapping
	@ResponseBody
	@Override
	public Object invoke() {
		if (!this.getDelegate().isEnabled()) {
			// Shouldn't happen
			return new ResponseEntity<Map<String, String>>(Collections.singletonMap(
					"message", "This endpoint is disabled"), HttpStatus.NOT_FOUND);
		}

		Health health = (Health) getDelegate().invoke();
		Status status = health.getStatus();
		if (this.statusMapping.containsKey(status.getCode())) {
			return new ResponseEntity<Health>(health, this.statusMapping.get(status
					.getCode()));
		}
		return health;
	}

}
