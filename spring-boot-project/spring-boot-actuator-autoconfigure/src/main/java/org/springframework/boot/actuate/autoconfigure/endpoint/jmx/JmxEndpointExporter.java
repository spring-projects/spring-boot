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

package org.springframework.boot.actuate.autoconfigure.endpoint.jmx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.jmx.EndpointMBeanRegistrar;
import org.springframework.boot.actuate.endpoint.jmx.JmxEndpointMBeanFactory;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperation;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperationResponseMapper;
import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxAnnotationEndpointDiscoverer;

/**
 * Exports all available {@link Endpoint} to a configurable {@link MBeanServer}.
 *
 * @author Stephane Nicoll
 */
class JmxEndpointExporter implements InitializingBean, DisposableBean {

	private final JmxAnnotationEndpointDiscoverer endpointDiscoverer;

	private final EndpointMBeanRegistrar endpointMBeanRegistrar;

	private final JmxEndpointMBeanFactory mBeanFactory;

	private Collection<ObjectName> registeredObjectNames;

	JmxEndpointExporter(JmxAnnotationEndpointDiscoverer endpointDiscoverer,
			EndpointMBeanRegistrar endpointMBeanRegistrar, ObjectMapper objectMapper) {
		this.endpointDiscoverer = endpointDiscoverer;
		this.endpointMBeanRegistrar = endpointMBeanRegistrar;
		DataConverter dataConverter = new DataConverter(objectMapper);
		this.mBeanFactory = new JmxEndpointMBeanFactory(dataConverter);
	}

	@Override
	public void afterPropertiesSet() {
		this.registeredObjectNames = registerEndpointMBeans();
	}

	private Collection<ObjectName> registerEndpointMBeans() {
		Collection<EndpointInfo<JmxOperation>> endpoints = this.endpointDiscoverer
				.discoverEndpoints();
		return this.mBeanFactory.createMBeans(endpoints).stream()
				.map(this.endpointMBeanRegistrar::registerEndpointMBean)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	@Override
	public void destroy() throws Exception {
		unregisterEndpointMBeans(this.registeredObjectNames);
	}

	private void unregisterEndpointMBeans(Collection<ObjectName> objectNames) {
		objectNames.forEach(this.endpointMBeanRegistrar::unregisterEndpointMbean);

	}

	static class DataConverter implements JmxOperationResponseMapper {

		private final ObjectMapper objectMapper;

		private final JavaType listObject;

		private final JavaType mapStringObject;

		DataConverter(ObjectMapper objectMapper) {
			this.objectMapper = (objectMapper == null ? new ObjectMapper()
					: objectMapper);
			this.listObject = this.objectMapper.getTypeFactory()
					.constructParametricType(List.class, Object.class);
			this.mapStringObject = this.objectMapper.getTypeFactory()
					.constructParametricType(Map.class, String.class, Object.class);
		}

		@Override
		public Object mapResponse(Object response) {
			if (response == null) {
				return null;
			}
			if (response instanceof String) {
				return response;
			}
			if (response.getClass().isArray() || response instanceof Collection) {
				return this.objectMapper.convertValue(response, this.listObject);
			}
			return this.objectMapper.convertValue(response, this.mapStringObject);
		}

		@Override
		public Class<?> mapResponseType(Class<?> responseType) {
			if (responseType.equals(String.class)) {
				return String.class;
			}
			if (responseType.isArray()
					|| Collection.class.isAssignableFrom(responseType)) {
				return List.class;
			}
			return Map.class;
		}

	}

}
