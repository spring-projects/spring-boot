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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMappingException;
import org.springframework.boot.actuate.endpoint.reflect.ParametersMissingException;
import org.springframework.util.ClassUtils;

/**
 * A {@link DynamicMBean} that invokes operations on an {@link EndpointInfo endpoint}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.0.0
 * @see EndpointMBeanInfoAssembler
 */
public class EndpointMBean implements DynamicMBean {

	private static final boolean REACTOR_PRESENT = ClassUtils.isPresent(
			"reactor.core.publisher.Mono", EndpointMBean.class.getClassLoader());

	private final Function<Object, Object> operationResponseConverter;

	private final EndpointMBeanInfo endpointInfo;

	EndpointMBean(Function<Object, Object> operationResponseConverter,
			EndpointMBeanInfo endpointInfo) {
		this.operationResponseConverter = operationResponseConverter;
		this.endpointInfo = endpointInfo;
	}

	/**
	 * Return the id of the related endpoint.
	 * @return the endpoint id
	 */
	public String getEndpointId() {
		return this.endpointInfo.getEndpointId();
	}

	@Override
	public MBeanInfo getMBeanInfo() {
		return this.endpointInfo.getMbeanInfo();
	}

	@Override
	public Object invoke(String actionName, Object[] params, String[] signature)
			throws MBeanException, ReflectionException {
		JmxOperation operation = this.endpointInfo.getOperations()
				.get(actionName);
		if (operation != null) {
			Map<String, Object> arguments = getArguments(params,
					operation.getParameters());
			try {
				Object result = operation.getInvoker().invoke(arguments);
				if (REACTOR_PRESENT) {
					result = ReactiveHandler.handle(result);
				}
				return this.operationResponseConverter.apply(result);
			}
			catch (ParametersMissingException | ParameterMappingException ex) {
				throw new IllegalArgumentException(ex.getMessage());
			}

		}
		throw new ReflectionException(new IllegalArgumentException(
				String.format("Endpoint with id '%s' has no operation named %s",
						this.endpointInfo.getEndpointId(), actionName)));
	}

	private Map<String, Object> getArguments(Object[] params,
			List<JmxEndpointOperationParameterInfo> parameters) {
		Map<String, Object> arguments = new HashMap<>();
		for (int i = 0; i < params.length; i++) {
			arguments.put(parameters.get(i).getName(), params[i]);
		}
		return arguments;
	}

	@Override
	public Object getAttribute(String attribute)
			throws AttributeNotFoundException, MBeanException, ReflectionException {
		throw new AttributeNotFoundException();
	}

	@Override
	public void setAttribute(Attribute attribute) throws AttributeNotFoundException,
			InvalidAttributeValueException, MBeanException, ReflectionException {
		throw new AttributeNotFoundException();
	}

	@Override
	public AttributeList getAttributes(String[] attributes) {
		return new AttributeList();
	}

	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		return new AttributeList();
	}

	private static class ReactiveHandler {

		public static Object handle(Object result) {
			if (result instanceof Mono) {
				return ((Mono<?>) result).block();
			}
			return result;
		}

	}

}
