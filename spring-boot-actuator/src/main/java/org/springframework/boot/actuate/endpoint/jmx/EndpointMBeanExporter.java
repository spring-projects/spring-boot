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

import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jmx.export.MBeanExportException;
import org.springframework.jmx.export.MBeanExporter;
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
public class EndpointMBeanExporter implements ApplicationListener<ContextRefreshedEvent> {

	private static final String DEFAULT_DOMAIN_NAME = ClassUtils
			.getPackageName(Endpoint.class);

	private static Log logger = LogFactory.getLog(EndpointMBeanExporter.class);

	private String domainName = DEFAULT_DOMAIN_NAME;

	private String key = "bean";

	public void setDomainName(String domainName) {
		Assert.notNull(domainName, "DomainName should not be null");
		this.domainName = domainName;
	}

	public void setKey(String key) {
		Assert.notNull(key, "Key should not be null");
		this.key = key;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		ApplicationContext applicationContext = event.getApplicationContext();
		try {
			MBeanExporter mbeanExporter = applicationContext.getBean(MBeanExporter.class);
			locateAndRegisterEndpoints(applicationContext, mbeanExporter);
		}
		catch (NoSuchBeanDefinitionException nsbde) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not obtain MBeanExporter. No Endpoint JMX export will be attemted.");
			}
		}
	}

	@SuppressWarnings({ "rawtypes" })
	protected void locateAndRegisterEndpoints(ApplicationContext applicationContext,
			MBeanExporter mbeanExporter) {
		Assert.notNull(applicationContext, "ApplicationContext should not be null");
		Map<String, Endpoint> endpoints = applicationContext
				.getBeansOfType(Endpoint.class);
		for (Map.Entry<String, Endpoint> endpointEntry : endpoints.entrySet()) {
			registerEndpoint(endpointEntry.getKey(), endpointEntry.getValue(),
					mbeanExporter);
		}
	}

	protected void registerEndpoint(String beanKey, Endpoint<?> endpoint,
			MBeanExporter mbeanExporter) {
		try {
			mbeanExporter.registerManagedResource(new EndpointMBean(endpoint),
					getObjectName(beanKey, endpoint));
		}
		catch (MBeanExportException e) {
			logger.error("Could not register MBean for endpoint [" + beanKey + "]", e);
		}
		catch (MalformedObjectNameException e) {
			logger.error("Could not register MBean for endpoint [" + beanKey + "]", e);
		}
	}

	protected ObjectName getObjectName(String beanKey, Endpoint<?> endpoint)
			throws MalformedObjectNameException {
		return ObjectNameManager.getInstance(this.domainName, this.key, beanKey);
	}

}
