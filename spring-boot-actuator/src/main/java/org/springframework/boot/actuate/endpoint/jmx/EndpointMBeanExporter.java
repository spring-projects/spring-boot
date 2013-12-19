/*
 * Copyright 2013 the original author or authors.
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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.jmx.export.MBeanExportException;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.support.JmxUtils;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link ApplicationListener} that registers all known {@link Endpoint}s with an
 * {@link MBeanServer} using the {@link MBeanExporter} located from the application
 * context.
 * 
 * @author Christian Dupuis
 */
public class EndpointMBeanExporter implements SmartLifecycle, ApplicationContextAware {

	private static final String DEFAULT_DOMAIN_NAME = ClassUtils
			.getPackageName(Endpoint.class);

	private static Log logger = LogFactory.getLog(EndpointMBeanExporter.class);

	private String domainName = DEFAULT_DOMAIN_NAME;

	private Set<Endpoint<?>> registeredEndpoints = new HashSet<Endpoint<?>>();

	private volatile boolean autoStartup = true;

	private volatile int phase = 0;

	private volatile boolean running = false;

	private final ReentrantLock lifecycleLock = new ReentrantLock();

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void setDomainName(String domainName) {
		Assert.notNull(domainName, "DomainName must not be null");
		this.domainName = domainName;
	}

	protected void doStart() {
		try {
			MBeanExporter mbeanExporter = this.applicationContext
					.getBean(MBeanExporter.class);
			locateAndRegisterEndpoints(mbeanExporter);
		}
		catch (NoSuchBeanDefinitionException nsbde) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not obtain MBeanExporter. No Endpoint JMX export will be attemted.");
			}
		}
	}

	@SuppressWarnings({ "rawtypes" })
	protected void locateAndRegisterEndpoints(MBeanExporter mbeanExporter) {
		Assert.notNull(mbeanExporter, "MBeanExporter must not be null");
		Map<String, Endpoint> endpoints = this.applicationContext
				.getBeansOfType(Endpoint.class);
		for (Map.Entry<String, Endpoint> endpointEntry : endpoints.entrySet()) {
			if (!this.registeredEndpoints.contains(endpointEntry.getValue())) {
				registerEndpoint(endpointEntry.getKey(), endpointEntry.getValue(),
						mbeanExporter);
				this.registeredEndpoints.add(endpointEntry.getValue());
			}
		}
	}

	protected void registerEndpoint(String beanKey, Endpoint<?> endpoint,
			MBeanExporter mbeanExporter) {
		try {
			mbeanExporter.registerManagedResource(new EndpointMBean(endpoint),
					getObjectName(beanKey, endpoint));
		}
		catch (MBeanExportException ex) {
			logger.error("Could not register MBean for endpoint [" + beanKey + "]", ex);
		}
		catch (MalformedObjectNameException ex) {
			logger.error("Could not register MBean for endpoint [" + beanKey + "]", ex);
		}
	}

	protected ObjectName getObjectName(String beanKey, Endpoint<?> endpoint)
			throws MalformedObjectNameException {
		// We have to be super careful to not create name clashes as multiple Boot
		// applications can potentially run in the same VM or MBeanServer. Therefore
		// append the object identity to the ObjectName.
		Hashtable<String, String> properties = new Hashtable<String, String>();
		properties.put("bean", beanKey);
		return JmxUtils.appendIdentityToObjectName(
				ObjectNameManager.getInstance(this.domainName, properties), endpoint);
	}

	// SmartLifeCycle implementation

	public final int getPhase() {
		return this.phase;
	}

	public final boolean isAutoStartup() {
		return this.autoStartup;
	}

	public final boolean isRunning() {
		this.lifecycleLock.lock();
		try {
			return this.running;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	public final void start() {
		this.lifecycleLock.lock();
		try {
			if (!this.running) {
				this.doStart();
				this.running = true;
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	public final void stop() {
		this.lifecycleLock.lock();
		try {
			if (this.running) {
				this.running = false;
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	public final void stop(Runnable callback) {
		this.lifecycleLock.lock();
		try {
			this.stop();
			callback.run();
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}
}
