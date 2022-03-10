/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Adapter to expose a {@link ExposableJmxEndpoint JMX endpoint} as a
 * {@link DynamicMBean}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public class EndpointMBean implements DynamicMBean {

	private static final boolean REACTOR_PRESENT = ClassUtils.isPresent("reactor.core.publisher.Mono",
			EndpointMBean.class.getClassLoader());

	private final JmxOperationResponseMapper responseMapper;

	private final ClassLoader classLoader;

	private final ExposableJmxEndpoint endpoint;

	private final MBeanInfo info;

	private final Map<String, JmxOperation> operations;

	EndpointMBean(JmxOperationResponseMapper responseMapper, ClassLoader classLoader, ExposableJmxEndpoint endpoint) {
		Assert.notNull(responseMapper, "ResponseMapper must not be null");
		Assert.notNull(endpoint, "Endpoint must not be null");
		this.responseMapper = responseMapper;
		this.classLoader = classLoader;
		this.endpoint = endpoint;
		this.info = new MBeanInfoFactory(responseMapper).getMBeanInfo(endpoint);
		this.operations = getOperations(endpoint);
	}

	private Map<String, JmxOperation> getOperations(ExposableJmxEndpoint endpoint) {
		Map<String, JmxOperation> operations = new HashMap<>();
		endpoint.getOperations().forEach((operation) -> operations.put(operation.getName(), operation));
		return Collections.unmodifiableMap(operations);
	}

	@Override
	public MBeanInfo getMBeanInfo() {
		return this.info;
	}

	@Override
	public Object invoke(String actionName, Object[] params, String[] signature)
			throws MBeanException, ReflectionException {
		JmxOperation operation = this.operations.get(actionName);
		if (operation == null) {
			String message = "Endpoint with id '" + this.endpoint.getEndpointId() + "' has no operation named "
					+ actionName;
			throw new ReflectionException(new IllegalArgumentException(message), message);
		}
		ClassLoader previousClassLoader = overrideThreadContextClassLoader(this.classLoader);
		try {
			return invoke(operation, params);
		}
		finally {
			overrideThreadContextClassLoader(previousClassLoader);
		}
	}

	private ClassLoader overrideThreadContextClassLoader(ClassLoader classLoader) {
		if (classLoader != null) {
			try {
				return ClassUtils.overrideThreadContextClassLoader(classLoader);
			}
			catch (SecurityException ex) {
				// can't set class loader, ignore it and proceed
			}
		}
		return null;
	}

	private Object invoke(JmxOperation operation, Object[] params) throws MBeanException, ReflectionException {
		try {
			String[] parameterNames = operation.getParameters().stream().map(JmxOperationParameter::getName)
					.toArray(String[]::new);
			Map<String, Object> arguments = getArguments(parameterNames, params);
			InvocationContext context = new InvocationContext(SecurityContext.NONE, arguments);
			Object result = operation.invoke(context);
			if (REACTOR_PRESENT) {
				result = ReactiveHandler.handle(result);
			}
			return this.responseMapper.mapResponse(result);
		}
		catch (InvalidEndpointRequestException ex) {
			throw new ReflectionException(new IllegalArgumentException(ex.getMessage()), ex.getMessage());
		}
		catch (Exception ex) {
			throw new MBeanException(translateIfNecessary(ex), ex.getMessage());
		}
	}

	private Exception translateIfNecessary(Exception exception) {
		if (exception.getClass().getName().startsWith("java.")) {
			return exception;
		}
		return new IllegalStateException(exception.getMessage());
	}

	private Map<String, Object> getArguments(String[] parameterNames, Object[] params) {
		Map<String, Object> arguments = new HashMap<>();
		for (int i = 0; i < params.length; i++) {
			arguments.put(parameterNames[i], params[i]);
		}
		return arguments;
	}

	@Override
	public Object getAttribute(String attribute)
			throws AttributeNotFoundException, MBeanException, ReflectionException {
		throw new AttributeNotFoundException("EndpointMBeans do not support attributes");
	}

	@Override
	public void setAttribute(Attribute attribute)
			throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		throw new AttributeNotFoundException("EndpointMBeans do not support attributes");
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

		static Object handle(Object result) {
			if (result instanceof Flux) {
				result = ((Flux<?>) result).collectList();
			}
			if (result instanceof Mono) {
				return ((Mono<?>) result).block();
			}
			return result;
		}

	}

}
