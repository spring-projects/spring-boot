/*
 * Copyright 2012-2015 the original author or authors.
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

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Adapter to expose {@link HealthEndpoint} as an {@link MvcEndpoint}.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 1.1.0
 */
public class HealthMvcEndpoint implements MvcEndpoint, EnvironmentAware {

	private final HealthEndpoint delegate;

	private final boolean secure;

	private Map<String, HttpStatus> statusMapping = new HashMap<String, HttpStatus>();

	private RelaxedPropertyResolver propertyResolver;

	private long lastAccess = 0;

	private Health cached;

	public HealthMvcEndpoint(HealthEndpoint delegate) {
		this(delegate, true);
	}

	public HealthMvcEndpoint(HealthEndpoint delegate, boolean secure) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
		this.secure = secure;
		setupDefaultStatusMapping();
	}

	private void setupDefaultStatusMapping() {
		addStatusMapping(Status.DOWN, HttpStatus.SERVICE_UNAVAILABLE);
		addStatusMapping(Status.OUT_OF_SERVICE, HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.propertyResolver = new RelaxedPropertyResolver(environment,
				"endpoints.health.");
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
	 * Add specific status mappings to the existing set.
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
	public Object invoke(Principal principal) {
		if (!this.delegate.isEnabled()) {
			// Shouldn't happen because the request mapping should not be registered
			return new ResponseEntity<Map<String, String>>(Collections.singletonMap(
					"message", "This endpoint is disabled"), HttpStatus.NOT_FOUND);
		}
		Health health = getHealth(principal);
		HttpStatus status = this.statusMapping.get(health.getStatus().getCode());
		if (status != null) {
			return new ResponseEntity<Health>(health, status);
		}
		return health;
	}

	private Health getHealth(Principal principal) {
		long accessTime = System.currentTimeMillis();
		if (isCacheStale(accessTime)) {
			this.lastAccess = accessTime;
			this.cached = this.delegate.invoke();
		}
		if (exposeHealthDetails(principal)) {
			return this.cached;
		}
		return Health.status(this.cached.getStatus()).build();
	}

	private boolean isCacheStale(long accessTime) {
		if (this.cached == null) {
			return true;
		}
		return (accessTime - this.lastAccess) >= this.delegate.getTimeToLive();
	}

	private boolean exposeHealthDetails(Principal principal) {
		return isSecure(principal) || isUnrestricted();
	}

	private boolean isSecure(Principal principal) {
		return (principal != null && !principal.getClass().getName()
				.contains("Anonymous"));
	}

	private boolean isUnrestricted() {
		Boolean sensitive = this.propertyResolver.getProperty("sensitive", Boolean.class);
		return !this.secure || Boolean.FALSE.equals(sensitive);
	}

	@Override
	public String getPath() {
		return "/" + this.delegate.getId();
	}

	@Override
	public boolean isSensitive() {
		return this.delegate.isSensitive();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class<? extends Endpoint> getEndpointType() {
		return this.delegate.getClass();
	}

}
