/*
 * Copyright 2012-2016 the original author or authors.
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

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.EndpointProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Abstract base class for {@link MvcEndpoint} implementations without a backing
 * {@link Endpoint}.
 *
 * @author Lari Hotari
 */
abstract class AbstractMvcEndpoint extends WebMvcConfigurerAdapter
		implements MvcEndpoint, EnvironmentAware {
	protected Environment environment;
	/**
	 * Endpoint URL path.
	 */
	@NotNull
	@Pattern(regexp = "/.*|^$", message = "Path must start with /")
	private String path;
	/**
	 * Enable the endpoint.
	 */
	private boolean enabled = true;
	/**
	 * Mark if the endpoint exposes sensitive information.
	 */
	private Boolean sensitive;

	protected boolean sensitiveDefault = true;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean isSensitive() {
		return EndpointProperties.isSensitive(this.environment, this.sensitive,
				sensitiveDefault);
	}

	public void setSensitive(Boolean sensitive) {
		this.sensitive = sensitive;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class<? extends Endpoint> getEndpointType() {
		return null;
	}
}
