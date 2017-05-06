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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
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
public class HealthMvcEndpoint extends AbstractEndpointMvcAdapter<HealthEndpoint> {

	private static final List<String> DEFAULT_ROLES = Arrays.asList("ROLE_ACTUATOR");

	private final boolean secure;

	private final List<String> roles;

	private Map<String, HttpStatus> statusMapping = new HashMap<>();

	private long lastAccess = 0;

	private Health cached;

	public HealthMvcEndpoint(HealthEndpoint delegate) {
		this(delegate, true);
	}

	public HealthMvcEndpoint(HealthEndpoint delegate, boolean secure) {
		this(delegate, secure, new ArrayList<>(DEFAULT_ROLES));
	}

	public HealthMvcEndpoint(HealthEndpoint delegate, boolean secure,
			List<String> roles) {
		super(delegate);
		Assert.notNull(roles, "Roles must not be null");
		this.secure = secure;
		this.roles = roles;
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
		this.statusMapping = new HashMap<>(statusMapping);
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
	public Object invoke(HttpServletRequest request, Principal principal) {
		if (!getDelegate().isEnabled()) {
			// Shouldn't happen because the request mapping should not be registered
			return getDisabledResponse();
		}
		Health health = getHealth(request, principal);
		HttpStatus status = getStatus(health);
		if (status != null) {
			return new ResponseEntity<>(health, status);
		}
		return health;
	}

	private HttpStatus getStatus(Health health) {
		String code = getUniformValue(health.getStatus().getCode());
		if (code != null) {
			return this.statusMapping.keySet().stream()
					.filter((key) -> code.equals(getUniformValue(key)))
					.map(this.statusMapping::get).findFirst().orElse(null);
		}
		return null;
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

	private Health getHealth(HttpServletRequest request, Principal principal) {
		long accessTime = System.currentTimeMillis();
		if (isCacheStale(accessTime)) {
			this.lastAccess = accessTime;
			this.cached = getDelegate().invoke();
		}
		if (exposeHealthDetails(request, principal)) {
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

	protected boolean exposeHealthDetails(HttpServletRequest request,
			Principal principal) {
		if (!this.secure) {
			return true;
		}
		List<String> roles = getRoles();
		for (String role : roles) {
			if (request.isUserInRole(role)) {
				return true;
			}
			if (isSpringSecurityAuthentication(principal)) {
				Authentication authentication = (Authentication) principal;
				for (GrantedAuthority authority : authentication.getAuthorities()) {
					String name = authority.getAuthority();
					if (role.equals(name)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private List<String> getRoles() {
		return this.roles;
	}

	private boolean isSpringSecurityAuthentication(Principal principal) {
		return ClassUtils.isPresent("org.springframework.security.core.Authentication",
				null) && principal instanceof Authentication;
	}

}
