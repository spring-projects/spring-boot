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

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.LoggersEndpoint;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jmx.export.MBeanExportException;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;
import org.springframework.jmx.export.metadata.InvalidMetadataException;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.export.naming.SelfNaming;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link SmartLifecycle} bean that registers all known {@link Endpoint}s with an
 * {@link MBeanServer} using the {@link MBeanExporter} located from the application
 * context.
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Vedran Pavic
 */
public class EndpointMBeanExporter extends MBeanExporter
		implements SmartLifecycle, ApplicationContextAware {

	/**
	 * The default JMX domain.
	 */
	public static final String DEFAULT_DOMAIN = "org.springframework.boot";

	private static final Log logger = LogFactory.getLog(EndpointMBeanExporter.class);

	private final AnnotationJmxAttributeSource attributeSource = new EndpointJmxAttributeSource();

	private final MetadataMBeanInfoAssembler assembler = new MetadataMBeanInfoAssembler(
			this.attributeSource);

	private final MetadataNamingStrategy defaultNamingStrategy = new MetadataNamingStrategy(
			this.attributeSource);

	private final Set<Class<?>> registeredEndpoints = new HashSet<Class<?>>();

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
		this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
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
	public void setEnsureUniqueRuntimeObjectNames(
			boolean ensureUniqueRuntimeObjectNames) {
		super.setEnsureUniqueRuntimeObjectNames(ensureUniqueRuntimeObjectNames);
		this.ensureUniqueRuntimeObjectNames = ensureUniqueRuntimeObjectNames;
	}

	public void setObjectNameStaticProperties(Properties objectNameStaticProperties) {
		this.objectNameStaticProperties = objectNameStaticProperties;
	}

	protected void doStart() {
		locateAndRegisterEndpoints();
	}

	protected void locateAndRegisterEndpoints() {
		registerJmxEndpoints(this.beanFactory.getBeansOfType(JmxEndpoint.class));
		registerEndpoints(this.beanFactory.getBeansOfType(Endpoint.class));
	}

	private void registerJmxEndpoints(Map<String, JmxEndpoint> endpoints) {
		for (Map.Entry<String, JmxEndpoint> entry : endpoints.entrySet()) {
			String name = entry.getKey();
			JmxEndpoint endpoint = entry.getValue();
			Class<?> type = (endpoint.getEndpointType() != null)
					? endpoint.getEndpointType() : endpoint.getClass();
			if (!this.registeredEndpoints.contains(type) && endpoint.isEnabled()) {
				try {
					registerBeanNameOrInstance(endpoint, name);
				}
				catch (MBeanExportException ex) {
					logger.error("Could not register JmxEndpoint [" + name + "]", ex);
				}
				this.registeredEndpoints.add(type);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void registerEndpoints(Map<String, Endpoint> endpoints) {
		for (Map.Entry<String, Endpoint> entry : endpoints.entrySet()) {
			String name = entry.getKey();
			Endpoint endpoint = entry.getValue();
			Class<?> type = endpoint.getClass();
			if (!this.registeredEndpoints.contains(type) && endpoint.isEnabled()) {
				registerEndpoint(name, endpoint);
				this.registeredEndpoints.add(type);
			}
		}
	}

	/**
	 * Register a regular {@link Endpoint} with the {@link MBeanServer}.
	 * @param beanName the bean name
	 * @param endpoint the endpoint to register
	 * @deprecated as of 1.5 in favor of direct {@link JmxEndpoint} registration or
	 * {@link #adaptEndpoint(String, Endpoint)}
	 */
	@Deprecated
	protected void registerEndpoint(String beanName, Endpoint<?> endpoint) {
		Class<?> type = endpoint.getClass();
		if (isAnnotatedWithManagedResource(type) || (type.isMemberClass()
				&& isAnnotatedWithManagedResource(type.getEnclosingClass()))) {
			// Endpoint is directly managed
			return;
		}
		JmxEndpoint jmxEndpoint = adaptEndpoint(beanName, endpoint);
		try {
			registerBeanNameOrInstance(jmxEndpoint, beanName);
		}
		catch (MBeanExportException ex) {
			logger.error("Could not register MBean for endpoint [" + beanName + "]", ex);
		}
	}

	private boolean isAnnotatedWithManagedResource(Class<?> type) {
		return AnnotationUtils.findAnnotation(type, ManagedResource.class) != null;
	}

	/**
	 * Adapt the given {@link Endpoint} to a {@link JmxEndpoint}.
	 * @param beanName the bean name
	 * @param endpoint the endpoint to adapt
	 * @return an adapted endpoint
	 */
	protected JmxEndpoint adaptEndpoint(String beanName, Endpoint<?> endpoint) {
		return getEndpointMBean(beanName, endpoint);
	}

	/**
	 * Get a {@link EndpointMBean} for the specified {@link Endpoint}.
	 * @param beanName the bean name
	 * @param endpoint the endpoint
	 * @return an {@link EndpointMBean}
	 * @deprecated as of 1.5 in favor of {@link #adaptEndpoint(String, Endpoint)}
	 */
	@Deprecated
	protected EndpointMBean getEndpointMBean(String beanName, Endpoint<?> endpoint) {
		if (endpoint instanceof ShutdownEndpoint) {
			return new ShutdownEndpointMBean(beanName, endpoint, this.objectMapper);
		}
		if (endpoint instanceof LoggersEndpoint) {
			return new LoggersEndpointMBean(beanName, endpoint, this.objectMapper);
		}
		return new DataEndpointMBean(beanName, endpoint, this.objectMapper);
	}

	@Override
	protected ObjectName getObjectName(Object bean, String beanKey)
			throws MalformedObjectNameException {
		if (bean instanceof SelfNaming) {
			return ((SelfNaming) bean).getObjectName();
		}
		if (bean instanceof JmxEndpoint) {
			return getObjectName((JmxEndpoint) bean, beanKey);
		}
		return this.defaultNamingStrategy.getObjectName(bean, beanKey);
	}

	private ObjectName getObjectName(JmxEndpoint jmxEndpoint, String beanKey)
			throws MalformedObjectNameException {
		StringBuilder builder = new StringBuilder();
		builder.append(this.domain);
		builder.append(":type=Endpoint");
		builder.append(",name=" + beanKey);
		if (parentContextContainsSameBean(this.applicationContext, beanKey)) {
			builder.append(",context="
					+ ObjectUtils.getIdentityHexString(this.applicationContext));
		}
		if (this.ensureUniqueRuntimeObjectNames) {
			builder.append(",identity=" + jmxEndpoint.getIdentity());
		}
		builder.append(getStaticNames());
		return ObjectNameManager.getInstance(builder.toString());
	}

	private boolean parentContextContainsSameBean(ApplicationContext applicationContext,
			String beanKey) {
		if (applicationContext.getParent() != null) {
			try {
				Object bean = this.applicationContext.getParent().getBean(beanKey);
				if (bean instanceof Endpoint || bean instanceof JmxEndpoint) {
					return true;
				}
			}
			catch (BeansException ex) {
				// Ignore and continue
			}
			return parentContextContainsSameBean(applicationContext.getParent(), beanKey);
		}
		return false;
	}

	private String getStaticNames() {
		if (this.objectNameStaticProperties.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();

		for (Entry<Object, Object> name : this.objectNameStaticProperties.entrySet()) {
			builder.append("," + name.getKey() + "=" + name.getValue());
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

	/**
	 * {@link JmxAttributeSource} for {@link JmxEndpoint JmxEndpoints}.
	 */
	private static class EndpointJmxAttributeSource extends AnnotationJmxAttributeSource {

		@Override
		public org.springframework.jmx.export.metadata.ManagedResource getManagedResource(
				Class<?> beanClass) throws InvalidMetadataException {
			Assert.state(super.getManagedResource(beanClass) == null,
					"@ManagedResource annotation found on JmxEndpoint " + beanClass);
			return new org.springframework.jmx.export.metadata.ManagedResource();
		}

	}

}
