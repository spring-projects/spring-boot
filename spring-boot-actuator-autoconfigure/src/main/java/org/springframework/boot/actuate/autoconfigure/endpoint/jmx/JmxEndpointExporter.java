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

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointProvider;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.jmx.EndpointMBean;
import org.springframework.boot.actuate.endpoint.jmx.EndpointMBeanRegistrar;
import org.springframework.boot.actuate.endpoint.jmx.JmxEndpointMBeanFactory;
import org.springframework.boot.actuate.endpoint.jmx.JmxEndpointOperation;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperationResponseMapper;

/**
 * Exports all available {@link Endpoint} to a configurable {@link MBeanServer}.
 *
 * @author Stephane Nicoll
 */
class JmxEndpointExporter implements InitializingBean, DisposableBean {

	private final EndpointProvider<JmxEndpointOperation> endpointProvider;

	private final EndpointMBeanRegistrar endpointMBeanRegistrar;

	private final JmxEndpointMBeanFactory mBeanFactory;

	private Collection<ObjectName> registeredObjectNames;

	JmxEndpointExporter(EndpointProvider<JmxEndpointOperation> endpointProvider,
			EndpointMBeanRegistrar endpointMBeanRegistrar, ObjectMapper objectMapper) {
		this.endpointProvider = endpointProvider;
		this.endpointMBeanRegistrar = endpointMBeanRegistrar;
		DataConverter dataConverter = new DataConverter(objectMapper);
		this.mBeanFactory = new JmxEndpointMBeanFactory(dataConverter);
	}

	@Override
	public void afterPropertiesSet() {
		this.registeredObjectNames = registerEndpointMBeans();
	}

	@Override
	public void destroy() throws Exception {
		unregisterEndpointMBeans(this.registeredObjectNames);
	}

	private Collection<ObjectName> registerEndpointMBeans() {
		List<ObjectName> objectNames = new ArrayList<>();
		Collection<EndpointMBean> mBeans = this.mBeanFactory
				.createMBeans(this.endpointProvider.getEndpoints());
		for (EndpointMBean mBean : mBeans) {
			objectNames.add(this.endpointMBeanRegistrar.registerEndpointMBean(mBean));
		}
		return objectNames;
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
