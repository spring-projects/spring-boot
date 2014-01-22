/*
 * Copyright 2013 the original author or authors.
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

import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simple wrapper around {@link Endpoint} implementations to enable JMX export.
 * 
 * @author Christian Dupuis
 */
@ManagedResource
public class EndpointMBean {

	private final Endpoint<?> endpoint;

	private final ObjectMapper mapper = new ObjectMapper();

	public EndpointMBean(String beanName, Endpoint<?> endpoint) {
		Assert.notNull(beanName, "BeanName must not be null");
		Assert.notNull(endpoint, "Endpoint must not be null");
		this.endpoint = endpoint;
	}

	@ManagedAttribute(description = "Returns the class of the underlying endpoint")
	public String getEndpointClass() {
		return ClassUtils.getQualifiedName(this.endpoint.getClass());
	}

	@ManagedAttribute(description = "Indicates whether the underlying endpoint exposes sensitive information")
	public boolean isSensitive() {
		return this.endpoint.isSensitive();
	}

	public Endpoint<?> getEndpoint() {
		return this.endpoint;
	}

	protected Object convert(Object result) {
		if (result == null) {
			return null;
		}

		if (result instanceof String) {
			return result;
		}

		if (result.getClass().isArray() || result instanceof List) {
			return this.mapper.convertValue(result, List.class);
		}

		return this.mapper.convertValue(result, Map.class);
	}

}
