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

package org.springframework.boot.admin;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;

/**
 * Register a {@link SpringApplicationAdminMXBean} implementation to the platform
 * {@link MBeanServer}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 1.3.0
 */
public class SpringApplicationAdminMXBeanRegistrar implements ApplicationContextAware, GenericApplicationListener,
		EnvironmentAware, InitializingBean, DisposableBean {

	private static final Log logger = LogFactory.getLog(SpringApplicationAdmin.class);

	private ConfigurableApplicationContext applicationContext;

	private Environment environment = new StandardEnvironment();

	private final ObjectName objectName;

	private boolean ready = false;

	private boolean embeddedWebApplication = false;

	/**
	 * Constructs a new SpringApplicationAdminMXBeanRegistrar with the specified name.
	 * @param name the name of the object
	 * @throws MalformedObjectNameException if the name is not a valid object name
	 */
	public SpringApplicationAdminMXBeanRegistrar(String name) throws MalformedObjectNameException {
		this.objectName = new ObjectName(name);
	}

	/**
	 * Sets the application context for the SpringApplicationAdminMXBeanRegistrar.
	 * @param applicationContext the application context to be set
	 * @throws BeansException if an error occurs while setting the application context
	 * @throws IllegalStateException if the provided application context does not
	 * implement ConfigurableApplicationContext
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.state(applicationContext instanceof ConfigurableApplicationContext,
				"ApplicationContext does not implement ConfigurableApplicationContext");
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}

	/**
	 * Sets the environment for the SpringApplicationAdminMXBeanRegistrar.
	 * @param environment the environment to be set
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Determines whether the specified event type is supported by this registrar.
	 * @param eventType the event type to check
	 * @return true if the event type is supported, false otherwise
	 */
	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		Class<?> type = eventType.getRawClass();
		if (type == null) {
			return false;
		}
		return ApplicationReadyEvent.class.isAssignableFrom(type)
				|| WebServerInitializedEvent.class.isAssignableFrom(type);
	}

	/**
	 * Determines if the specified source type is supported.
	 * @param sourceType the source type to check
	 * @return true if the source type is supported, false otherwise
	 */
	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return true;
	}

	/**
	 * This method is called when an application event is triggered. It checks the type of
	 * the event and calls the corresponding handler method.
	 * @param event the application event
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationReadyEvent readyEvent) {
			onApplicationReadyEvent(readyEvent);
		}
		if (event instanceof WebServerInitializedEvent initializedEvent) {
			onWebServerInitializedEvent(initializedEvent);
		}
	}

	/**
	 * Returns the order of this bean in the bean execution order. The order is set to the
	 * highest precedence.
	 * @return the order of this bean
	 */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	/**
	 * Sets the ready flag to true when the application is ready.
	 * @param event the ApplicationReadyEvent triggered when the application is ready
	 */
	void onApplicationReadyEvent(ApplicationReadyEvent event) {
		if (this.applicationContext.equals(event.getApplicationContext())) {
			this.ready = true;
		}
	}

	/**
	 * This method is called when the web server is initialized. It checks if the
	 * application context of the event matches the current application context. If they
	 * match, it sets the embeddedWebApplication flag to true.
	 * @param event The WebServerInitializedEvent containing the initialized web server
	 * information.
	 */
	void onWebServerInitializedEvent(WebServerInitializedEvent event) {
		if (this.applicationContext.equals(event.getApplicationContext())) {
			this.embeddedWebApplication = true;
		}
	}

	/**
	 * This method is called after all properties have been set for the
	 * SpringApplicationAdminMXBeanRegistrar class. It registers the
	 * SpringApplicationAdmin MBean with the MBeanServer using the provided objectName. If
	 * debug logging is enabled, it logs a message indicating the registration of the
	 * MBean.
	 * @throws Exception if an error occurs during the registration process
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		server.registerMBean(new SpringApplicationAdmin(), this.objectName);
		if (logger.isDebugEnabled()) {
			logger.debug("Application Admin MBean registered with name '" + this.objectName + "'");
		}
	}

	/**
	 * This method is called when the application is being destroyed. It unregisters the
	 * MBean associated with the SpringApplicationAdminMXBeanRegistrar class from the
	 * platform MBean server.
	 * @throws Exception if an error occurs while unregistering the MBean
	 */
	@Override
	public void destroy() throws Exception {
		ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.objectName);
	}

	/**
	 * SpringApplicationAdmin class.
	 */
	private final class SpringApplicationAdmin implements SpringApplicationAdminMXBean {

		/**
		 * Returns a boolean value indicating whether the application is ready.
		 * @return true if the application is ready, false otherwise
		 */
		@Override
		public boolean isReady() {
			return SpringApplicationAdminMXBeanRegistrar.this.ready;
		}

		/**
		 * Returns a boolean value indicating whether the application is an embedded web
		 * application.
		 * @return true if the application is an embedded web application, false otherwise
		 */
		@Override
		public boolean isEmbeddedWebApplication() {
			return SpringApplicationAdminMXBeanRegistrar.this.embeddedWebApplication;
		}

		/**
		 * Retrieves the value of the property with the specified key.
		 * @param key the key of the property to retrieve
		 * @return the value of the property with the specified key
		 */
		@Override
		public String getProperty(String key) {
			return SpringApplicationAdminMXBeanRegistrar.this.environment.getProperty(key);
		}

		/**
		 * Shuts down the application.
		 *
		 * This method is called to initiate the shutdown of the application. It logs a
		 * message indicating that the shutdown has been requested and then closes the
		 * application context.
		 *
		 * @see SpringApplicationAdminMXBeanRegistrar
		 * @see ApplicationContext#close()
		 */
		@Override
		public void shutdown() {
			logger.info("Application shutdown requested.");
			SpringApplicationAdminMXBeanRegistrar.this.applicationContext.close();
		}

	}

}
