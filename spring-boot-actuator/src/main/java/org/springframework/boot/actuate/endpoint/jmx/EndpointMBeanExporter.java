/*
 * Copyright 2013-2015 the original author or authors.
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
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jmx.export.MBeanExportException;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.export.naming.SelfNaming;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link ApplicationListener} that registers all known {@link Endpoint}s with an
 * {@link MBeanServer} using the {@link MBeanExporter} located from the application
 * context.
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
 */
public class EndpointMBeanExporter extends MBeanExporter implements SmartLifecycle,
		BeanFactoryAware, ApplicationContextAware {

	public static final String DEFAULT_DOMAIN = "org.springframework.boot";

	private static Log logger = LogFactory.getLog(EndpointMBeanExporter.class);

	private final AnnotationJmxAttributeSource attributeSource = new AnnotationJmxAttributeSource();

	private final MetadataMBeanInfoAssembler assembler = new MetadataMBeanInfoAssembler(
			this.attributeSource);

	private final MetadataNamingStrategy defaultNamingStrategy = new MetadataNamingStrategy(
			this.attributeSource);

	private final Set<Endpoint<?>> registeredEndpoints = new HashSet<Endpoint<?>>();

	private volatile boolean autoStartup = true;

	private volatile int phase = 0;

	private volatile boolean running = false;

	private final ReentrantLock lifecycleLock = new ReentrantLock();

	private ApplicationContext applicationContext;

	private ListableBeanFactory beanFactory;

	private String domain = DEFAULT_DOMAIN;

	private boolean ensureUniqueRuntimeObjectNames = false;

	private Properties objectNameStaticProperties = new Properties();

	private final ObjectMapper objectMapper;

	/**
	 * Create a new {@link EndpointMBeanExporter} instance.
	 */
	public EndpointMBeanExporter() {
		this(null);
	}

	/**
	 * Create a new {@link EndpointMBeanExporter} instance.
	 * @param objectMapper the object mapper
	 */
	public EndpointMBeanExporter(ObjectMapper objectMapper) {
		this.objectMapper = (objectMapper == null ? new ObjectMapper() : objectMapper);
		setAutodetect(false);
		setNamingStrategy(this.defaultNamingStrategy);
		setAssembler(this.assembler);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		if (beanFactory instanceof ListableBeanFactory) {
			this.beanFactory = (ListableBeanFactory) beanFactory;
		}
		else {
			logger.warn("EndpointMBeanExporter not running in a ListableBeanFactory: "
					+ "autodetection of Endpoints not available.");
		}
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	@Override
	public void setEnsureUniqueRuntimeObjectNames(boolean ensureUniqueRuntimeObjectNames) {
		super.setEnsureUniqueRuntimeObjectNames(ensureUniqueRuntimeObjectNames);
		this.ensureUniqueRuntimeObjectNames = ensureUniqueRuntimeObjectNames;
	}

	public void setObjectNameStaticProperties(Properties objectNameStaticProperties) {
		this.objectNameStaticProperties = objectNameStaticProperties;
	}

	protected void doStart() {
		locateAndRegisterEndpoints();
	}

	@SuppressWarnings({ "rawtypes" })
	protected void locateAndRegisterEndpoints() {
		Map<String, Endpoint> endpoints = this.beanFactory.getBeansOfType(Endpoint.class);
		for (Map.Entry<String, Endpoint> endpointEntry : endpoints.entrySet()) {
			if (!this.registeredEndpoints.contains(endpointEntry.getValue())) {
				registerEndpoint(endpointEntry.getKey(), endpointEntry.getValue());
				this.registeredEndpoints.add(endpointEntry.getValue());
			}
		}
	}

	protected void registerEndpoint(String beanName, Endpoint<?> endpoint) {
		@SuppressWarnings("rawtypes")
		Class<? extends Endpoint> type = endpoint.getClass();
		if (AnnotationUtils.findAnnotation(type, ManagedResource.class) != null) {
			// Already managed
			return;
		}
		if (type.isMemberClass()
				&& AnnotationUtils.findAnnotation(type.getEnclosingClass(),
						ManagedResource.class) != null) {
			// Nested class with @ManagedResource in parent
			return;
		}
		try {
			registerBeanNameOrInstance(getEndpointMBean(beanName, endpoint), beanName);
		}
		catch (MBeanExportException ex) {
			logger.error("Could not register MBean for endpoint [" + beanName + "]", ex);
		}
	}

	protected EndpointMBean getEndpointMBean(String beanName, Endpoint<?> endpoint) {
		if (endpoint instanceof ShutdownEndpoint) {
			return new ShutdownEndpointMBean(beanName, endpoint, this.objectMapper);
		}
		return new DataEndpointMBean(beanName, endpoint, this.objectMapper);
	}

	@Override
	protected ObjectName getObjectName(Object bean, String beanKey)
			throws MalformedObjectNameException {
		if (bean instanceof SelfNaming) {
			return ((SelfNaming) bean).getObjectName();
		}

		if (bean instanceof EndpointMBean) {
			StringBuilder builder = new StringBuilder();
			builder.append(this.domain);
			builder.append(":type=Endpoint");
			builder.append(",name=" + beanKey);
			if (parentContextContainsSameBean(this.applicationContext, beanKey)) {
				builder.append(",context="
						+ ObjectUtils.getIdentityHexString(this.applicationContext));
			}
			if (this.ensureUniqueRuntimeObjectNames) {
				builder.append(",identity="
						+ ObjectUtils.getIdentityHexString(((EndpointMBean) bean)
								.getEndpoint()));
			}
			builder.append(getStaticNames());
			return ObjectNameManager.getInstance(builder.toString());
		}

		return this.defaultNamingStrategy.getObjectName(bean, beanKey);
	}

	private boolean parentContextContainsSameBean(ApplicationContext applicationContext,
			String beanKey) {
		if (applicationContext.getParent() != null) {
			try {
				this.applicationContext.getParent().getBean(beanKey, Endpoint.class);
				return true;
			}
			catch (BeansException ex) {
				return parentContextContainsSameBean(applicationContext.getParent(),
						beanKey);
			}
		}
		return false;
	}

	private String getStaticNames() {
		if (this.objectNameStaticProperties.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();

		for (Object key : this.objectNameStaticProperties.keySet()) {
			builder.append("," + key + "=" + this.objectNameStaticProperties.get(key));
		}
		return builder.toString();
	}

	@Override
	public final int getPhase() {
		return this.phase;
	}

	@Override
	public final boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public final boolean isRunning() {
		this.lifecycleLock.lock();
		try {
			return this.running;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
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

	@Override
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

	@Override
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
