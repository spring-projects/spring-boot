/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.context;

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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;

/**
 * Register a {@link SpringApplicationLifecycleMXBean} implementation to the platform
 * {@link MBeanServer}.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class SpringApplicationLifecycleRegistrar implements ApplicationContextAware,
		InitializingBean, DisposableBean, ApplicationListener<ApplicationReadyEvent> {

	private static final Log logger = LogFactory.getLog(SpringApplicationLifecycle.class);

	private ConfigurableApplicationContext applicationContext;

	private final ObjectName objectName;

	private boolean ready = false;

	public SpringApplicationLifecycleRegistrar(String name)
			throws MalformedObjectNameException {
		this.objectName = new ObjectName(name);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		Assert.state(applicationContext instanceof ConfigurableApplicationContext,
				"ApplicationContext does not implement ConfigurableApplicationContext");
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		this.ready = true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		server.registerMBean(new SpringApplicationLifecycle(), this.objectName);
		if (logger.isDebugEnabled()) {
			logger.debug("Application lifecycle MBean registered with name '"
					+ this.objectName + "'");
		}
	}

	@Override
	public void destroy() throws Exception {
		ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.objectName);
	}

	private class SpringApplicationLifecycle implements SpringApplicationLifecycleMXBean {

		@Override
		public boolean isReady() {
			return SpringApplicationLifecycleRegistrar.this.ready;
		}

		@Override
		public void shutdown() {
			logger.info("Application shutdown requested.");
			SpringApplicationLifecycleRegistrar.this.applicationContext.close();
		}
	}

}
