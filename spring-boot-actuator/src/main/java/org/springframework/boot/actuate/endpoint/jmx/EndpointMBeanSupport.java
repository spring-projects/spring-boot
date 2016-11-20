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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.util.Assert;

/**
 * Abstract base class for JMX endpoint implementations.
 *
 * @author Vedran Pavic
 * @since 1.5.0
 */
public abstract class EndpointMBeanSupport {

	private final ObjectMapper mapper;

	private final JavaType listObject;

	private final JavaType mapStringObject;

	public EndpointMBeanSupport(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.mapper = objectMapper;
		this.listObject = objectMapper.getTypeFactory()
				.constructParametricType(List.class, Object.class);
		this.mapStringObject = objectMapper.getTypeFactory()
				.constructParametricType(Map.class, String.class, Object.class);
	}

	@ManagedAttribute(description = "Indicates whether the underlying endpoint exposes sensitive information")
	public abstract boolean isSensitive();

	@ManagedAttribute(description = "Returns the class of the underlying endpoint")
	public abstract String getEndpointClass();

	protected Object convert(Object result) {
		if (result == null) {
			return null;
		}
		if (result instanceof String) {
			return result;
		}
		if (result.getClass().isArray() || result instanceof List) {
			return this.mapper.convertValue(result, this.listObject);
		}
		return this.mapper.convertValue(result, this.mapStringObject);
	}

}
