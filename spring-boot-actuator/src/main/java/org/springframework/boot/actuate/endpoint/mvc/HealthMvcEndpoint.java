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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.bind.RelaxedNames;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Adapter to expose {@link HealthEndpoint} as an {@link MvcEndpoint}.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Madhura Bhave
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "endpoints.health")
public class HealthMvcEndpoint extends AbstractEndpointMvcAdapter<HealthEndpoint>
		implements EnvironmentAware {

	private final boolean secure;

	private Map<String, HttpStatus> statusMapping = new HashMap<String, HttpStatus>();

	private RelaxedPropertyResolver securityPropertyResolver;

	private long lastAccess = 0;

	private Health cached;

	public HealthMvcEndpoint(HealthEndpoint delegate) {
		this(delegate, true);
	}

	public HealthMvcEndpoint(HealthEndpoint delegate, boolean secure) {
		super(delegate);
		this.secure = secure;
		setupDefaultStatusMapping();
	}

	private void setupDefaultStatusMapping() {
		addStatusMapping(Status.DOWN, HttpStatus.SERVICE_UNAVAILABLE);
		addStatusMapping(Status.OUT_OF_SERVICE, HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.securityPropertyResolver = new RelaxedPropertyResolver(environment,
				"management.security.");
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

	@ActuatorGetMapping
	@ResponseBody
	public Object invoke(HttpServletRequest request) {
		if (!getDelegate().isEnabled()) {
			// Shouldn't happen because the request mapping should not be registered
			return getDisabledResponse();
		}
		Health health = getHealth(request);
		HttpStatus status = getStatus(health);
		if (status != null) {
			return new ResponseEntity<Health>(health, status);
		}
		return health;
	}

	private HttpStatus getStatus(Health health) {
		String code = health.getStatus().getCode();
		if (code != null) {
			code = code.toLowerCase().replace('_', '-');
			for (String candidate : RelaxedNames.forCamelCase(code)) {
				HttpStatus status = this.statusMapping.get(candidate);
				if (status != null) {
					return status;
				}
			}
		}
		return null;
	}

	private Health getHealth(HttpServletRequest request) {
		long accessTime = System.currentTimeMillis();
		if (isCacheStale(accessTime)) {
			this.lastAccess = accessTime;
			this.cached = getDelegate().invoke();
		}
		if (exposeHealthDetails(request)) {
			return this.cached;
		}
		return Health.status(this.cached.getStatus()).build();
	}

	private boolean isCacheStale(long accessTime) {
		if (this.cached == null) {
			return true;
		}
		return (accessTime - this.lastAccess) >= getDelegate().getTimeToLive();
	}

	protected boolean exposeHealthDetails(HttpServletRequest request) {
		if (!this.secure) {
			return true;
		}
		String[] roles = StringUtils.commaDelimitedListToStringArray(
				this.securityPropertyResolver.getProperty("roles", "ROLE_ACTUATOR"));
		roles = StringUtils.trimArrayElements(roles);
		for (String role : roles) {
			if (request.isUserInRole(role) || request.isUserInRole("ROLE_" + role)) {
				return true;
			}
		}
		return false;
	}

}
