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

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jmx.JmxException;
import org.springframework.jmx.export.MBeanExportException;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.util.Assert;

/**
 * JMX Registrar for {@link EndpointMBean}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 * @see EndpointObjectNameFactory
 */
public class EndpointMBeanRegistrar {

	private static final Log logger = LogFactory.getLog(EndpointMBeanRegistrar.class);

	private final MBeanServer mBeanServer;

	private final EndpointObjectNameFactory objectNameFactory;

	/**
	 * Create a new instance with the {@link MBeanExporter} and
	 * {@link EndpointObjectNameFactory} to use.
	 * @param mBeanServer the mbean exporter
	 * @param objectNameFactory the {@link ObjectName} factory
	 */
	public EndpointMBeanRegistrar(MBeanServer mBeanServer,
			EndpointObjectNameFactory objectNameFactory) {
		Assert.notNull(mBeanServer, "MBeanServer must not be null");
		Assert.notNull(objectNameFactory, "ObjectNameFactory must not be null");
		this.mBeanServer = mBeanServer;
		this.objectNameFactory = objectNameFactory;
	}

	/**
	 * Register the specified {@link EndpointMBean} and return its {@link ObjectName}.
	 * @param endpoint the endpoint to register
	 * @return the {@link ObjectName} used to register the {@code endpoint}
	 */
	public ObjectName registerEndpointMBean(EndpointMBean endpoint) {
		Assert.notNull(endpoint, "Endpoint must not be null");
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Registering endpoint with id '" + endpoint.getEndpointId()
						+ "' to the JMX domain");
			}
			ObjectName objectName = this.objectNameFactory.generate(endpoint);
			this.mBeanServer.registerMBean(endpoint, objectName);
			return objectName;
		}
		catch (MalformedObjectNameException ex) {
			throw new IllegalStateException("Invalid ObjectName for endpoint with id '"
					+ endpoint.getEndpointId() + "'", ex);
		}
		catch (Exception ex) {
			throw new MBeanExportException(
					"Failed to register MBean for endpoint with id '"
							+ endpoint.getEndpointId() + "'",
					ex);
		}
	}

	/**
	 * Unregister the specified {@link ObjectName} if necessary.
	 * @param objectName the {@link ObjectName} of the endpoint to unregister
	 * @return {@code true} if the endpoint was unregistered, {@code false} if no such
	 * endpoint was found
	 */
	public boolean unregisterEndpointMbean(ObjectName objectName) {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Unregister endpoint with ObjectName '" + objectName + "' "
						+ "from the JMX domain");
			}
			this.mBeanServer.unregisterMBean(objectName);
			return true;
		}
		catch (InstanceNotFoundException ex) {
			return false;
		}
		catch (MBeanRegistrationException ex) {
			throw new JmxException(
					"Failed to unregister MBean with ObjectName '" + objectName + "'",
					ex);
		}
	}

}
