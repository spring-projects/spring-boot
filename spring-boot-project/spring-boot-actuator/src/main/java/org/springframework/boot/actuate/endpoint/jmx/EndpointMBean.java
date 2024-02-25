/*
 * Copyright 2012-2024 the original author or authors.
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

	/**
     * Constructs a new EndpointMBean with the specified response mapper, class loader, and endpoint.
     * 
     * @param responseMapper the JmxOperationResponseMapper used to map JMX operation responses
     * @param classLoader the ClassLoader used to load classes for the endpoint
     * @param endpoint the ExposableJmxEndpoint representing the JMX endpoint
     * @throws IllegalArgumentException if responseMapper or endpoint is null
     */
    EndpointMBean(JmxOperationResponseMapper responseMapper, ClassLoader classLoader, ExposableJmxEndpoint endpoint) {
		Assert.notNull(responseMapper, "ResponseMapper must not be null");
		Assert.notNull(endpoint, "Endpoint must not be null");
		this.responseMapper = responseMapper;
		this.classLoader = classLoader;
		this.endpoint = endpoint;
		this.info = new MBeanInfoFactory(responseMapper).getMBeanInfo(endpoint);
		this.operations = getOperations(endpoint);
	}

	/**
     * Returns a map of JMX operations for the given {@link ExposableJmxEndpoint}.
     *
     * @param endpoint the {@link ExposableJmxEndpoint} to get the operations from
     * @return a map of JMX operations, where the key is the operation name and the value is the {@link JmxOperation} object
     */
    private Map<String, JmxOperation> getOperations(ExposableJmxEndpoint endpoint) {
		Map<String, JmxOperation> operations = new HashMap<>();
		endpoint.getOperations().forEach((operation) -> operations.put(operation.getName(), operation));
		return Collections.unmodifiableMap(operations);
	}

	/**
     * Returns the MBean information for this EndpointMBean.
     *
     * @return the MBean information
     */
    @Override
	public MBeanInfo getMBeanInfo() {
		return this.info;
	}

	/**
     * Invokes the specified action on the EndpointMBean.
     * 
     * @param actionName the name of the action to be invoked
     * @param params the parameters to be passed to the action
     * @param signature the signature of the action
     * @return the result of the action invocation
     * @throws MBeanException if an exception occurs while invoking the action
     * @throws ReflectionException if the specified action does not exist
     */
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

	/**
     * Overrides the thread context class loader with the specified class loader.
     * 
     * @param classLoader the class loader to set as the thread context class loader
     * @return the previous thread context class loader, or null if it couldn't be set
     */
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

	/**
     * Invokes the specified JMX operation with the given parameters.
     * 
     * @param operation the JmxOperation to invoke
     * @param params the parameters to pass to the operation
     * @return the result of the operation
     * @throws MBeanException if an exception occurs during the operation invocation
     * @throws ReflectionException if a reflection-related exception occurs during the operation invocation
     */
    private Object invoke(JmxOperation operation, Object[] params) throws MBeanException, ReflectionException {
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

	/**
     * Translates the given exception if necessary.
     * 
     * @param exception the exception to be translated
     * @return the translated exception if necessary, otherwise the original exception
     * @throws IllegalStateException if the given exception is not a Java exception
     */
    private Exception translateIfNecessary(Exception exception) {
		if (exception.getClass().getName().startsWith("java.")) {
			return exception;
		}
		return new IllegalStateException(exception.getMessage());
	}

	/**
     * Returns a map of arguments with their corresponding parameter names.
     * 
     * @param parameterNames an array of parameter names
     * @param params an array of parameters
     * @return a map of arguments with their corresponding parameter names
     */
    private Map<String, Object> getArguments(String[] parameterNames, Object[] params) {
		Map<String, Object> arguments = new HashMap<>();
		for (int i = 0; i < params.length; i++) {
			arguments.put(parameterNames[i], params[i]);
		}
		return arguments;
	}

	/**
     * Retrieves the value of the specified attribute from the EndpointMBean.
     * 
     * @param attribute the name of the attribute to retrieve
     * @return the value of the specified attribute
     * @throws AttributeNotFoundException if the specified attribute is not found in the EndpointMBean
     * @throws MBeanException if a problem occurs while retrieving the attribute
     * @throws ReflectionException if a reflection-related problem occurs while retrieving the attribute
     */
    @Override
	public Object getAttribute(String attribute)
			throws AttributeNotFoundException, MBeanException, ReflectionException {
		throw new AttributeNotFoundException("EndpointMBeans do not support attributes");
	}

	/**
     * Sets the attribute for the EndpointMBean.
     * 
     * @param attribute the attribute to be set
     * @throws AttributeNotFoundException if the attribute is not found
     * @throws InvalidAttributeValueException if the attribute value is invalid
     * @throws MBeanException if a problem occurs while setting the attribute
     * @throws ReflectionException if a reflection problem occurs
     * @throws UnsupportedOperationException if EndpointMBeans do not support attributes
     */
    @Override
	public void setAttribute(Attribute attribute)
			throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		throw new AttributeNotFoundException("EndpointMBeans do not support attributes");
	}

	/**
     * Retrieves the attributes specified in the given array.
     * 
     * @param attributes an array of attribute names
     * @return an AttributeList containing the retrieved attributes
     */
    @Override
	public AttributeList getAttributes(String[] attributes) {
		return new AttributeList();
	}

	/**
     * Sets the attributes for the EndpointMBean.
     * 
     * @param attributes the list of attributes to be set
     * @return the updated AttributeList
     */
    @Override
	public AttributeList setAttributes(AttributeList attributes) {
		return new AttributeList();
	}

	/**
     * ReactiveHandler class.
     */
    private static final class ReactiveHandler {

		/**
         * Handles the result object by converting it to a list if it is a Flux, or blocking and returning the value if it is a Mono.
         * 
         * @param result the result object to be handled
         * @return the handled result object
         */
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
