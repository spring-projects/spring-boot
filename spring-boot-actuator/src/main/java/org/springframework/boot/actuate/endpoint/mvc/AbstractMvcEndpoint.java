/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.EndpointProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Abstract base class for {@link MvcEndpoint} implementations without a backing
 * {@link Endpoint}.
 *
 * @author Phillip Webb
 * @author Lari Hotari
 * @since 1.4.0
 */
public abstract class AbstractMvcEndpoint extends WebMvcConfigurerAdapter
		implements MvcEndpoint, EnvironmentAware {

	private Environment environment;

	/**
	 * Endpoint URL path.
	 */
	private String path;

	/**
	 * Enable the endpoint.
	 */
	private Boolean enabled;

	/**
	 * Mark if the endpoint exposes sensitive information.
	 */
	private Boolean sensitive;

	private final boolean sensitiveDefault;

	public AbstractMvcEndpoint(String path, boolean sensitive) {
		setPath(path);
		this.sensitiveDefault = sensitive;
	}

	public AbstractMvcEndpoint(String path, boolean sensitive, boolean enabled) {
		setPath(path);
		this.sensitiveDefault = sensitive;
		this.enabled = enabled;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	protected final Environment getEnvironment() {
		return this.environment;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		Assert.notNull(path, "Path must not be null");
		Assert.isTrue(path.isEmpty() || path.startsWith("/"),
				"Path must start with / or be empty");
		this.path = path;
	}

	public boolean isEnabled() {
		return EndpointProperties.isEnabled(this.environment, this.enabled);
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean isSensitive() {
		return EndpointProperties.isSensitive(this.environment, this.sensitive,
				this.sensitiveDefault);
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
