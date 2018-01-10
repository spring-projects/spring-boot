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

package org.springframework.boot.actuate.endpoint.jmx.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.annotation.AnnotationEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperation;
import org.springframework.boot.actuate.endpoint.reflect.OperationMethodInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;

/**
 * Discovers the {@link Endpoint endpoints} in an {@link ApplicationContext} with
 * {@link EndpointJmxExtension JMX extensions} applied to them.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class JmxAnnotationEndpointDiscoverer
		extends AnnotationEndpointDiscoverer<String, JmxOperation> {

	static final AnnotationJmxAttributeSource jmxAttributeSource = new AnnotationJmxAttributeSource();

	/**
	 * Creates a new {@link JmxAnnotationEndpointDiscoverer} that will discover
	 * {@link Endpoint endpoints} and {@link EndpointJmxExtension jmx extensions} using
	 * the given {@link ApplicationContext}.
	 * @param applicationContext the application context
	 * @param parameterMapper the {@link ParameterMapper} used to convert arguments when
	 * an operation is invoked
	 * @param invokerAdvisors advisors used to add additional invoker advise
	 * @param filters filters that must match for an endpoint to be exposed
	 */
	public JmxAnnotationEndpointDiscoverer(ApplicationContext applicationContext,
			ParameterMapper parameterMapper,
			Collection<? extends OperationMethodInvokerAdvisor> invokerAdvisors,
			Collection<? extends EndpointFilter<JmxOperation>> filters) {
		super(applicationContext, new JmxEndpointOperationFactory(),
				JmxOperation::getOperationName, parameterMapper, invokerAdvisors,
				filters);
	}

	@Override
	protected void verify(Collection<DiscoveredEndpoint> exposedEndpoints) {
		List<List<JmxOperation>> clashes = new ArrayList<>();
		exposedEndpoints.forEach((exposedEndpoint) -> clashes
				.addAll(exposedEndpoint.findDuplicateOperations().values()));
		if (!clashes.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append(
					String.format("Found multiple JMX operations with the same name:%n"));
			clashes.forEach((clash) -> {
				message.append("    ").append(clash.get(0).getOperationName())
						.append(String.format(":%n"));
				clash.forEach((operation) -> message.append("        ")
						.append(String.format("%s%n", operation)));
			});
			throw new IllegalStateException(message.toString());
		}
	}

}
