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

import java.util.Collection;
import java.util.Collections;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.JmxException;
import org.springframework.jmx.export.MBeanExportException;
import org.springframework.util.Assert;

/**
 * Exports {@link ExposableJmxEndpoint JMX endpoints} to a {@link MBeanServer}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
public class JmxEndpointExporter implements InitializingBean, DisposableBean, BeanClassLoaderAware {

	private static final Log logger = LogFactory.getLog(JmxEndpointExporter.class);

	private ClassLoader classLoader;

	private final MBeanServer mBeanServer;

	private final EndpointObjectNameFactory objectNameFactory;

	private final JmxOperationResponseMapper responseMapper;

	private final Collection<ExposableJmxEndpoint> endpoints;

	private Collection<ObjectName> registered;

	/**
	 * Creates a new instance of JmxEndpointExporter with the specified parameters.
	 * @param mBeanServer the MBeanServer to use for exporting the endpoints
	 * @param objectNameFactory the ObjectNameFactory to use for generating the ObjectName
	 * for each endpoint
	 * @param responseMapper the JmxOperationResponseMapper to use for mapping operation
	 * responses
	 * @param endpoints the collection of endpoints to be exported
	 * @throws IllegalArgumentException if any of the parameters is null
	 */
	public JmxEndpointExporter(MBeanServer mBeanServer, EndpointObjectNameFactory objectNameFactory,
			JmxOperationResponseMapper responseMapper, Collection<? extends ExposableJmxEndpoint> endpoints) {
		Assert.notNull(mBeanServer, "MBeanServer must not be null");
		Assert.notNull(objectNameFactory, "ObjectNameFactory must not be null");
		Assert.notNull(responseMapper, "ResponseMapper must not be null");
		Assert.notNull(endpoints, "Endpoints must not be null");
		this.mBeanServer = mBeanServer;
		this.objectNameFactory = objectNameFactory;
		this.responseMapper = responseMapper;
		this.endpoints = Collections.unmodifiableCollection(endpoints);
	}

	/**
	 * Sets the class loader to be used for loading beans in the JmxEndpointExporter.
	 * @param classLoader the class loader to be set
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * This method is called after all properties have been set for the
	 * JmxEndpointExporter class. It registers the JMX endpoint by calling the register()
	 * method.
	 */
	@Override
	public void afterPropertiesSet() {
		this.registered = register();
	}

	/**
	 * This method is called when the JmxEndpointExporter is being destroyed. It
	 * unregisters the registered JMX beans.
	 * @throws Exception if an error occurs during the unregistration process
	 */
	@Override
	public void destroy() throws Exception {
		unregister(this.registered);
	}

	/**
	 * Registers all endpoints in the JmxEndpointExporter.
	 * @return A collection of ObjectName representing the registered endpoints.
	 */
	private Collection<ObjectName> register() {
		return this.endpoints.stream().map(this::register).toList();
	}

	/**
	 * Registers an ExposableJmxEndpoint with the MBean server.
	 * @param endpoint the ExposableJmxEndpoint to register
	 * @return the ObjectName of the registered MBean
	 * @throws IllegalArgumentException if the endpoint is null
	 * @throws IllegalStateException if the ObjectName for the endpoint is invalid
	 * @throws MBeanExportException if the registration of the MBean fails
	 */
	private ObjectName register(ExposableJmxEndpoint endpoint) {
		Assert.notNull(endpoint, "Endpoint must not be null");
		try {
			ObjectName name = this.objectNameFactory.getObjectName(endpoint);
			EndpointMBean mbean = new EndpointMBean(this.responseMapper, this.classLoader, endpoint);
			this.mBeanServer.registerMBean(mbean, name);
			return name;
		}
		catch (MalformedObjectNameException ex) {
			throw new IllegalStateException("Invalid ObjectName for " + getEndpointDescription(endpoint), ex);
		}
		catch (Exception ex) {
			throw new MBeanExportException("Failed to register MBean for " + getEndpointDescription(endpoint), ex);
		}
	}

	/**
	 * Unregisters the specified collection of object names from the JMX endpoint
	 * exporter.
	 * @param objectNames the collection of object names to unregister
	 */
	private void unregister(Collection<ObjectName> objectNames) {
		objectNames.forEach(this::unregister);
	}

	/**
	 * Unregisters an MBean with the specified ObjectName from the JMX domain.
	 * @param objectName the ObjectName of the MBean to unregister
	 * @throws JmxException if an error occurs while unregistering the MBean
	 */
	private void unregister(ObjectName objectName) {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Unregister endpoint with ObjectName '" + objectName + "' from the JMX domain");
			}
			this.mBeanServer.unregisterMBean(objectName);
		}
		catch (InstanceNotFoundException ex) {
			// Ignore and continue
		}
		catch (MBeanRegistrationException ex) {
			throw new JmxException("Failed to unregister MBean with ObjectName '" + objectName + "'", ex);
		}
	}

	/**
	 * Returns the description of the specified JMX endpoint.
	 * @param endpoint the JMX endpoint to get the description for
	 * @return the description of the JMX endpoint
	 */
	private String getEndpointDescription(ExposableJmxEndpoint endpoint) {
		return "endpoint '" + endpoint.getEndpointId() + "'";
	}

}
