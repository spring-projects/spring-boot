/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;
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

	private final @Nullable ClassLoader classLoader;

	private final ExposableJmxEndpoint endpoint;

	private final MBeanInfo info;

	private final Map<String, JmxOperation> operations;

	EndpointMBean(JmxOperationResponseMapper responseMapper, @Nullable ClassLoader classLoader,
			ExposableJmxEndpoint endpoint) {
		Assert.notNull(responseMapper, "'responseMapper' must not be null");
		Assert.notNull(endpoint, "'endpoint' must not be null");
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
	public @Nullable Object invoke(String actionName, Object[] params, String[] signature)
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

	private @Nullable ClassLoader overrideThreadContextClassLoader(@Nullable ClassLoader classLoader) {
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

	private @Nullable Object invoke(JmxOperation operation, Object[] params)
			throws MBeanException, ReflectionException {
		try {
			String[] parameterNames = operation.getParameters()
				.stream()
				.map(JmxOperationParameter::getName)
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

	private static final class ReactiveHandler {

		static @Nullable Object handle(@Nullable Object result) {
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
