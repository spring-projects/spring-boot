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

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.EndpointInfo;

/**
 * A factory for creating JMX MBeans for endpoint operations.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class JmxEndpointMBeanFactory {

	private final EndpointMBeanInfoAssembler assembler;

	private final JmxOperationResponseMapper resultMapper;

	/**
	 * Create a new {@link JmxEndpointMBeanFactory} instance that will use the given
	 * {@code responseMapper} to convert an operation's response to a JMX-friendly form.
	 * @param responseMapper the response mapper
	 */
	public JmxEndpointMBeanFactory(JmxOperationResponseMapper responseMapper) {
		this.assembler = new EndpointMBeanInfoAssembler(responseMapper);
		this.resultMapper = responseMapper;
	}

	/**
	 * Creates MBeans for the given {@code endpoints}.
	 * @param endpoints the endpoints
	 * @return the MBeans
	 */
	public Collection<EndpointMBean> createMBeans(
			Collection<EndpointInfo<JmxOperation>> endpoints) {
		return endpoints.stream().map(this::createMBean).collect(Collectors.toList());
	}

	private EndpointMBean createMBean(EndpointInfo<JmxOperation> endpointInfo) {
		EndpointMBeanInfo endpointMBeanInfo = this.assembler
				.createEndpointMBeanInfo(endpointInfo);
		return new EndpointMBean(this.resultMapper::mapResponse, endpointMBeanInfo);
	}

}
