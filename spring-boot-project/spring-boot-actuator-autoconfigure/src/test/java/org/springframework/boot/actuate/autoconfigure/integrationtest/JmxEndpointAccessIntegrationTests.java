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

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.jmx.JmxEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.exchanges.HttpExchangesAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for controlling access to endpoints exposed by JMX.
 *
 * @author Andy Wilkinson
 */
class JmxEndpointAccessIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class, EndpointAutoConfiguration.class,
				JmxEndpointAutoConfiguration.class, HealthContributorAutoConfiguration.class,
				HttpExchangesAutoConfiguration.class))
		.withUserConfiguration(CustomJmxEndpoint.class)
		.withPropertyValues("spring.jmx.enabled=true")
		.withConfiguration(AutoConfigurations.of(EndpointAutoConfigurationClasses.ALL));

	@Test
	void accessIsUnrestrictedByDefault() {
		this.contextRunner.withPropertyValues("management.endpoints.jmx.exposure.include=*").run((context) -> {
			MBeanServer mBeanServer = context.getBean(MBeanServer.class);
			assertThat(hasOperation(mBeanServer, "beans", "beans")).isTrue();
			assertThat(hasOperation(mBeanServer, "customjmx", "read")).isTrue();
			assertThat(hasOperation(mBeanServer, "customjmx", "write")).isTrue();
			assertThat(hasOperation(mBeanServer, "customjmx", "delete")).isTrue();
		});
	}

	@Test
	void accessCanBeReadOnlyByDefault() {
		this.contextRunner
			.withPropertyValues("management.endpoints.jmx.exposure.include=*",
					"management.endpoints.access.default=READ_ONLY")
			.run((context) -> {
				MBeanServer mBeanServer = context.getBean(MBeanServer.class);
				assertThat(hasOperation(mBeanServer, "beans", "beans")).isTrue();
				assertThat(hasOperation(mBeanServer, "customjmx", "read")).isTrue();
				assertThat(hasOperation(mBeanServer, "customjmx", "write")).isFalse();
				assertThat(hasOperation(mBeanServer, "customjmx", "delete")).isFalse();
			});
	}

	@Test
	void accessCanBeNoneByDefault() {
		this.contextRunner
			.withPropertyValues("management.endpoints.jmx.exposure.include=*",
					"management.endpoints.access.default=NONE")
			.run((context) -> {
				MBeanServer mBeanServer = context.getBean(MBeanServer.class);
				assertThat(hasOperation(mBeanServer, "beans", "beans")).isFalse();
				assertThat(hasOperation(mBeanServer, "customjmx", "read")).isFalse();
				assertThat(hasOperation(mBeanServer, "customjmx", "write")).isFalse();
				assertThat(hasOperation(mBeanServer, "customjmx", "delete")).isFalse();
			});
	}

	@Test
	void accessForOneEndpointCanOverrideTheDefaultAccess() {
		this.contextRunner
			.withPropertyValues("management.endpoints.jmx.exposure.include=*",
					"management.endpoints.access.default=NONE", "management.endpoint.customjmx.access=UNRESTRICTED")
			.run((context) -> {
				MBeanServer mBeanServer = context.getBean(MBeanServer.class);
				assertThat(hasOperation(mBeanServer, "beans", "beans")).isFalse();
				assertThat(hasOperation(mBeanServer, "customjmx", "read")).isTrue();
				assertThat(hasOperation(mBeanServer, "customjmx", "write")).isTrue();
				assertThat(hasOperation(mBeanServer, "customjmx", "delete")).isTrue();
			});
	}

	@Test
	void accessCanBeCappedAtReadOnly() {
		this.contextRunner
			.withPropertyValues("management.endpoints.jmx.exposure.include=*",
					"management.endpoints.access.default=UNRESTRICTED",
					"management.endpoints.access.max-permitted=READ_ONLY")
			.run((context) -> {
				MBeanServer mBeanServer = context.getBean(MBeanServer.class);
				assertThat(hasOperation(mBeanServer, "beans", "beans")).isTrue();
				assertThat(hasOperation(mBeanServer, "customjmx", "read")).isTrue();
				assertThat(hasOperation(mBeanServer, "customjmx", "write")).isFalse();
				assertThat(hasOperation(mBeanServer, "customjmx", "delete")).isFalse();
			});
	}

	@Test
	void accessCanBeCappedAtNone() {
		this.contextRunner.withPropertyValues("management.endpoints.jmx.exposure.include=*",
				"management.endpoints.access.default=UNRESTRICTED", "management.endpoints.access.max-permitted=NONE")
			.run((context) -> {
				MBeanServer mBeanServer = context.getBean(MBeanServer.class);
				assertThat(hasOperation(mBeanServer, "beans", "beans")).isFalse();
				assertThat(hasOperation(mBeanServer, "customjmx", "read")).isFalse();
				assertThat(hasOperation(mBeanServer, "customjmx", "write")).isFalse();
				assertThat(hasOperation(mBeanServer, "customjmx", "delete")).isFalse();
			});
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

	private boolean hasOperation(MBeanServer mbeanServer, String endpoint, String operationName) {
		try {
			for (MBeanOperationInfo operation : mbeanServer.getMBeanInfo(getDefaultObjectName(endpoint))
				.getOperations()) {
				if (operation.getName().equals(operationName)) {
					return true;
				}
			}
		}
		catch (Exception ex) {
			// Continue
		}
		return false;
	}

	@JmxEndpoint(id = "customjmx")
	static class CustomJmxEndpoint {

		@ReadOperation
		String read() {
			return "read";
		}

		@WriteOperation
		String write() {
			return "write";
		}

		@DeleteOperation
		String delete() {
			return "delete";
		}

	}

}
