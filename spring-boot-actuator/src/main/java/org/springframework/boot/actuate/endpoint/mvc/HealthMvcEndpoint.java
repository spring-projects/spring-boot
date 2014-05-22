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

	private Map<String, HttpStatus> statusMapping;

	public HealthMvcEndpoint(HealthEndpoint delegate) {
		super(delegate);
		setupDefaultStatusMapping();
	}

	public void setStatusMapping(Map<String, HttpStatus> statusMapping) {
		Assert.notNull(statusMapping, "StatusMapping must not be null");
		this.statusMapping = statusMapping;
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
			return new ResponseEntity<Health>(health,
					this.statusMapping.get(status.getCode()));
		}
		return health;
	}

	private void setupDefaultStatusMapping() {
		this.statusMapping = new HashMap<String, HttpStatus>();
		this.statusMapping.put(Status.DOWN.getCode(), HttpStatus.SERVICE_UNAVAILABLE);
		this.statusMapping.put(Status.OUT_OF_SERVICE.getCode(),
				HttpStatus.SERVICE_UNAVAILABLE);
	}
}
