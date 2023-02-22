/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.jmx.JmxEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.exchanges.HttpExchangesAutoConfiguration;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for endpoints over JMX.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class JmxEndpointIntegrationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class, EndpointAutoConfiguration.class,
				JmxEndpointAutoConfiguration.class, HealthContributorAutoConfiguration.class,
				HttpExchangesAutoConfiguration.class))
		.withUserConfiguration(HttpExchangeRepositoryConfiguration.class, AuditEventRepositoryConfiguration.class)
		.withPropertyValues("spring.jmx.enabled=true")
		.withConfiguration(AutoConfigurations.of(EndpointAutoConfigurationClasses.ALL));

	@Test
	void jmxEndpointsExposeHealthByDefault() {
		this.contextRunner.run((context) -> {
			MBeanServer mBeanServer = context.getBean(MBeanServer.class);
			checkEndpointMBeans(mBeanServer, new String[] { "health" }, new String[] { "beans", "conditions",
					"configprops", "env", "info", "mappings", "threaddump", "httpexchanges", "shutdown" });
		});
	}

	@Test
	void jmxEndpointsAreExposedWhenLazyInitializationIsEnabled() {
		this.contextRunner.withPropertyValues("management.endpoints.jmx.exposure.include:*")
			.withBean(LazyInitializationBeanFactoryPostProcessor.class, LazyInitializationBeanFactoryPostProcessor::new)
			.run((context) -> {
				MBeanServer mBeanServer = context.getBean(MBeanServer.class);
				checkEndpointMBeans(mBeanServer, new String[] { "beans", "conditions", "configprops", "env", "health",
						"info", "mappings", "threaddump", "httpexchanges" }, new String[] { "shutdown" });
			});
	}

	@Test
	void jmxEndpointsCanBeExcluded() {
		this.contextRunner.withPropertyValues("management.endpoints.jmx.exposure.exclude:*").run((context) -> {
			MBeanServer mBeanServer = context.getBean(MBeanServer.class);
			checkEndpointMBeans(mBeanServer, new String[0], new String[] { "beans", "conditions", "configprops", "env",
					"health", "mappings", "shutdown", "threaddump", "httpexchanges" });

		});
	}

	@Test
	void singleJmxEndpointCanBeExposed() {
		this.contextRunner.withPropertyValues("management.endpoints.jmx.exposure.include=beans").run((context) -> {
			MBeanServer mBeanServer = context.getBean(MBeanServer.class);
			checkEndpointMBeans(mBeanServer, new String[] { "beans" }, new String[] { "conditions", "configprops",
					"env", "health", "mappings", "shutdown", "threaddump", "httpexchanges" });
		});
	}

	private void checkEndpointMBeans(MBeanServer mBeanServer, String[] enabledEndpoints, String[] disabledEndpoints) {
		for (String enabledEndpoint : enabledEndpoints) {
			assertThat(isRegistered(mBeanServer, getDefaultObjectName(enabledEndpoint)))
				.as(String.format("Endpoint %s", enabledEndpoint))
				.isTrue();
		}
		for (String disabledEndpoint : disabledEndpoints) {
			assertThat(isRegistered(mBeanServer, getDefaultObjectName(disabledEndpoint)))
				.as(String.format("Endpoint %s", disabledEndpoint))
				.isFalse();
		}
	}

	private boolean isRegistered(MBeanServer mBeanServer, ObjectName objectName) {
		try {
			getMBeanInfo(mBeanServer, objectName);
			return true;
		}
		catch (InstanceNotFoundException ex) {
			return false;
		}
	}

	private MBeanInfo getMBeanInfo(MBeanServer mBeanServer, ObjectName objectName) throws InstanceNotFoundException {
		try {
			return mBeanServer.getMBeanInfo(objectName);
		}
		catch (ReflectionException | IntrospectionException ex) {
			throw new IllegalStateException("Failed to retrieve MBeanInfo for ObjectName " + objectName, ex);
		}
	}

	private ObjectName getDefaultObjectName(String endpointId) {
		return getObjectName("org.springframework.boot", endpointId);
	}

	private ObjectName getObjectName(String domain, String endpointId) {
		try {
			return new ObjectName(
					String.format("%s:type=Endpoint,name=%s", domain, StringUtils.capitalize(endpointId)));
		}
		catch (MalformedObjectNameException ex) {
			throw new IllegalStateException("Invalid object name", ex);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HttpExchangeRepositoryConfiguration {

		@Bean
		InMemoryHttpExchangeRepository httpExchangeRepository() {
			return new InMemoryHttpExchangeRepository();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AuditEventRepositoryConfiguration {

		@Bean
		InMemoryAuditEventRepository auditEventRepository() {
			return new InMemoryAuditEventRepository();
		}

	}

}
