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

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * {@link MvcEndpoint} for the actuator. Uses content negotiation to provide access to the
 * HAL browser (when on the classpath), and to HAL-formatted JSON.
 *
 * @author Dave Syer
 * @author Phil Webb
 * @author Andy Wilkinson
 */
@ConfigurationProperties("endpoints.actuator")
public class ActuatorHalJsonEndpoint extends WebMvcConfigurerAdapter implements
		MvcEndpoint {

	/**
	 * Endpoint URL path.
	 */
	@NotNull
	@Pattern(regexp = "^$|/[^/]*", message = "Path must be empty or start with /")
	private String path;

	/**
	 * Enable security on the endpoint.
	 */
	private boolean sensitive = false;

	/**
	 * Enable the endpoint.
	 */
	private boolean enabled = true;

	private final ManagementServerProperties management;

	public ActuatorHalJsonEndpoint(ManagementServerProperties management) {
		this.management = management;
		if (StringUtils.hasText(management.getContextPath())) {
			this.path = "";
		}
		else {
			this.path = "/actuator";
		}
	}

	@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResourceSupport links() {
		return new ResourceSupport();
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public boolean isSensitive() {
		return this.sensitive;
	}

	public void setSensitive(boolean sensitive) {
		this.sensitive = sensitive;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public Class<? extends Endpoint<?>> getEndpointType() {
		return null;
	}

	protected final ManagementServerProperties getManagement() {
		return this.management;
	}

}
