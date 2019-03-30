/*
 * Copyright 2012-2018 the original author or authors.
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
import java.util.stream.Collectors;

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
public class JmxEndpointExporter
		implements InitializingBean, DisposableBean, BeanClassLoaderAware {

	private static final Log logger = LogFactory.getLog(JmxEndpointExporter.class);

	private ClassLoader classLoader;

	private final MBeanServer mBeanServer;

	private final EndpointObjectNameFactory objectNameFactory;

	private final JmxOperationResponseMapper responseMapper;

	private final Collection<ExposableJmxEndpoint> endpoints;

	private Collection<ObjectName> registered;

	public JmxEndpointExporter(MBeanServer mBeanServer,
			EndpointObjectNameFactory objectNameFactory,
			JmxOperationResponseMapper responseMapper,
			Collection<? extends ExposableJmxEndpoint> endpoints) {
		Assert.notNull(mBeanServer, "MBeanServer must not be null");
		Assert.notNull(objectNameFactory, "ObjectNameFactory must not be null");
		Assert.notNull(responseMapper, "ResponseMapper must not be null");
		Assert.notNull(endpoints, "Endpoints must not be null");
		this.mBeanServer = mBeanServer;
		this.objectNameFactory = objectNameFactory;
		this.responseMapper = responseMapper;
		this.endpoints = Collections.unmodifiableCollection(endpoints);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() {
		this.registered = register();
	}

	@Override
	public void destroy() throws Exception {
		unregister(this.registered);
	}

	private Collection<ObjectName> register() {
		return this.endpoints.stream().map(this::register).collect(Collectors.toList());
	}

	private ObjectName register(ExposableJmxEndpoint endpoint) {
		Assert.notNull(endpoint, "Endpoint must not be null");
		try {
			ObjectName name = this.objectNameFactory.getObjectName(endpoint);
			EndpointMBean mbean = new EndpointMBean(this.responseMapper, this.classLoader,
					endpoint);
			this.mBeanServer.registerMBean(mbean, name);
			return name;
		}
		catch (MalformedObjectNameException ex) {
			throw new IllegalStateException(
					"Invalid ObjectName for " + getEndpointDescription(endpoint), ex);
		}
		catch (Exception ex) {
			throw new MBeanExportException(
					"Failed to register MBean for " + getEndpointDescription(endpoint),
					ex);
		}
	}

	private void unregister(Collection<ObjectName> objectNames) {
		objectNames.forEach(this::unregister);
	}

	private void unregister(ObjectName objectName) {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Unregister endpoint with ObjectName '" + objectName + "' "
						+ "from the JMX domain");
			}
			this.mBeanServer.unregisterMBean(objectName);
		}
		catch (InstanceNotFoundException ex) {
			// Ignore and continue
		}
		catch (MBeanRegistrationException ex) {
			throw new JmxException(
					"Failed to unregister MBean with ObjectName '" + objectName + "'",
					ex);
		}
	}

	private String getEndpointDescription(ExposableJmxEndpoint endpoint) {
		return "endpoint '" + endpoint.getEndpointId() + "'";
	}

}
