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

package org.springframework.boot.actuate.endpoint.jmx;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.EndpointProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Abstract base class for JMX endpoint implementations without a backing
 * {@link Endpoint}.
 *
 * @author Vedran Pavic
 * @since 1.5.0
 */
public abstract class AbstractEndpointMBean extends EndpointMBeanSupport
		implements EnvironmentAware {

	private Environment environment;

	/**
	 * Enable the endpoint.
	 */
	private Boolean enabled;

	/**
	 * Mark if the endpoint exposes sensitive information.
	 */
	private Boolean sensitive;

	private final boolean sensitiveDefault;

	public AbstractEndpointMBean(ObjectMapper objectMapper, boolean sensitive) {
		super(objectMapper);
		this.sensitiveDefault = sensitive;
	}

	public AbstractEndpointMBean(ObjectMapper objectMapper, boolean sensitive,
			boolean enabled) {
		super(objectMapper);
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
	public String getEndpointClass() {
		return null;
	}

}
