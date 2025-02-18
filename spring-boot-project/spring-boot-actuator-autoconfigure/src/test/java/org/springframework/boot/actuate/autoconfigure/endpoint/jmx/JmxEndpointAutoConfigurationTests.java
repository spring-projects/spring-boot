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

package org.springframework.boot.actuate.autoconfigure.endpoint.jmx;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.jmx.EndpointObjectNameFactory;
import org.springframework.boot.actuate.endpoint.jmx.JmxEndpointExporter;
import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpointDiscoverer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link JmxEndpointAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class JmxEndpointAutoConfigurationTests {

	private static final ContextConsumer<ConfigurableApplicationContext> NO_OPERATION = (context) -> {
	};

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(EndpointAutoConfiguration.class, JmxAutoConfiguration.class,
				JmxEndpointAutoConfiguration.class))
		.withUserConfiguration(TestEndpoint.class);

	private final MBeanServer mBeanServer = mock(MBeanServer.class);

	@Test
	void jmxEndpointWithoutJmxSupportNotAutoConfigured() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(MBeanServer.class)
			.doesNotHaveBean(JmxEndpointDiscoverer.class)
			.doesNotHaveBean(JmxEndpointExporter.class));
	}

	@Test
	void jmxEndpointWithJmxSupportAutoConfigured() {
		this.contextRunner.withPropertyValues("spring.jmx.enabled=true")
			.with(mockMBeanServer())
			.run((context) -> assertThat(context).hasSingleBean(JmxEndpointDiscoverer.class)
				.hasSingleBean(JmxEndpointExporter.class));
	}

	@Test
	void jmxEndpointWithCustomEndpointObjectNameFactory() {
		EndpointObjectNameFactory factory = mock(EndpointObjectNameFactory.class);
		this.contextRunner
			.withPropertyValues("spring.jmx.enabled=true", "management.endpoints.jmx.exposure.include=test")
			.with(mockMBeanServer())
			.withBean(EndpointObjectNameFactory.class, () -> factory)
			.run((context) -> then(factory).should()
				.getObjectName(assertArg((jmxEndpoint) -> assertThat(jmxEndpoint.getEndpointId().toLowerCaseString())
					.isEqualTo("test"))));
	}

	@Test
	void jmxEndpointWithContextHierarchyGeneratesUniqueNamesForEachEndpoint() throws Exception {
		given(this.mBeanServer.queryNames(any(), any()))
			.willReturn(new HashSet<>(Arrays.asList(new ObjectName("test:test=test"))));
		ArgumentCaptor<ObjectName> objectName = ArgumentCaptor.forClass(ObjectName.class);
		ApplicationContextRunner jmxEnabledContextRunner = this.contextRunner
			.withPropertyValues("spring.jmx.enabled=true", "management.endpoints.jmx.exposure.include=test");
		jmxEnabledContextRunner.with(mockMBeanServer()).run((parent) -> {
			jmxEnabledContextRunner.withParent(parent).run(NO_OPERATION);
			jmxEnabledContextRunner.withParent(parent).run(NO_OPERATION);
		});
		then(this.mBeanServer).should(times(3)).registerMBean(any(Object.class), objectName.capture());
		Set<ObjectName> uniqueValues = new HashSet<>(objectName.getAllValues());
		assertThat(uniqueValues).hasSize(3);
		assertThat(uniqueValues).allMatch((name) -> name.getDomain().equals("org.springframework.boot"));
		assertThat(uniqueValues).allMatch((name) -> name.getKeyProperty("type").equals("Endpoint"));
		assertThat(uniqueValues).allMatch((name) -> name.getKeyProperty("name").equals("Test"));
		assertThat(uniqueValues).allMatch((name) -> name.getKeyProperty("context") != null);
	}

	private Function<ApplicationContextRunner, ApplicationContextRunner> mockMBeanServer() {
		return (ctxRunner) -> ctxRunner.withBean("mbeanServer", MBeanServer.class, () -> this.mBeanServer);
	}

	@Endpoint(id = "test")
	static class TestEndpoint {

		@ReadOperation
		String hello() {
			return "hello world";
		}

	}

}
