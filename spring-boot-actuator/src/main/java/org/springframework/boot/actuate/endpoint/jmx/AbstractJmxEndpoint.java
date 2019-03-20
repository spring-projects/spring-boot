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

package org.springframework.boot.actuate.endpoint.jmx;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.EndpointProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.ObjectUtils;

/**
 * Abstract base class for {@link JmxEndpoint} implementations without a backing
 * {@link Endpoint}.
 *
 * @author Vedran Pavic
 * @author Phillip Webb
 * @since 1.5.0
 */
public abstract class AbstractJmxEndpoint implements JmxEndpoint, EnvironmentAware {

	private final DataConverter dataConverter;

	private Environment environment;

	/**
	 * Enable the endpoint.
	 */
	private Boolean enabled;

	public AbstractJmxEndpoint(ObjectMapper objectMapper) {
		this.dataConverter = new DataConverter(objectMapper);
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	protected final Environment getEnvironment() {
		return this.environment;
	}

	@Override
	public boolean isEnabled() {
		return EndpointProperties.isEnabled(this.environment, this.enabled);
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public String getIdentity() {
		return ObjectUtils.getIdentityHexString(this);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class<? extends Endpoint> getEndpointType() {
		return null;
	}

	/**
	 * Convert the given data into JSON.
	 * @param data the source data
	 * @return the JSON representation
	 */
	protected Object convert(Object data) {
		return this.dataConverter.convert(data);
	}

}
