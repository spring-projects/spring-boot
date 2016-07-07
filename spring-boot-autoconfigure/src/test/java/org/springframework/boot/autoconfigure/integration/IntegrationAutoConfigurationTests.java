/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.integration;

import java.util.Arrays;
import java.util.List;

import javax.management.MBeanServer;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.support.channel.HeaderChannelRegistry;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link IntegrationAutoConfiguration}.
 *
 * @author Artem Bilan
 * @author Stephane Nicoll
 */
public class IntegrationAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
			if (this.context.getParent() != null) {
				((ConfigurableApplicationContext) this.context.getParent()).close();
			}
		}
	}

	@Test
	public void integrationIsAvailable() {
		load();
		assertThat(this.context.getBean(HeaderChannelRegistry.class)).isNotNull();
	}

	@Test
	public void parentContext() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JmxAutoConfiguration.class,
				IntegrationAutoConfiguration.class);
		this.context.refresh();
		AnnotationConfigApplicationContext parent = this.context;
		this.context = new AnnotationConfigApplicationContext();
		this.context.setParent(parent);
		this.context.register(JmxAutoConfiguration.class,
				IntegrationAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(HeaderChannelRegistry.class)).isNotNull();
		((ConfigurableApplicationContext) this.context.getParent()).close();
		this.context.close();
	}

	@Test
	public void jmxIntegrationEnabledByDefault() {
		load();
		MBeanServer mBeanServer = this.context.getBean(MBeanServer.class);
		assertDomains(mBeanServer, true, "org.springframework.integration",
				"org.springframework.integration.monitor");
	}

	@Test
	public void disableJmxIntegration() {
		load("spring.jmx.enabled=false");
		assertThat(this.context.getBeansOfType(MBeanServer.class)).hasSize(0);
	}

	@Test
	public void customizeJmxDomain() {
		load("SPRING_JMX_DEFAULT_DOMAIN=org.foo");
		MBeanServer mBeanServer = this.context.getBean(MBeanServer.class);
		assertDomains(mBeanServer, true, "org.foo");
		assertDomains(mBeanServer, false, "org.springframework.integration",
				"org.springframework.integration.monitor");
	}

	@Test
	public void primaryExporterIsAllowed() {
		load(CustomMBeanExporter.class);
		assertThat(this.context.getBeansOfType(MBeanExporter.class)).hasSize(2);
		assertThat(this.context.getBean(MBeanExporter.class)).isSameAs(
				this.context.getBean("myMBeanExporter"));
	}

	private static void assertDomains(MBeanServer mBeanServer, boolean expected,
			String... domains) {
		List<String> actual = Arrays.asList(mBeanServer.getDomains());
		for (String domain : domains) {
			assertThat(actual.contains(domain)).isEqualTo(expected);
		}
	}

	public void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		if (config != null) {
			ctx.register(config);
		}
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx, environment);
		ctx.register(JmxAutoConfiguration.class, IntegrationAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration
	static class CustomMBeanExporter {

		@Bean
		@Primary
		public MBeanExporter myMBeanExporter() {
			return mock(MBeanExporter.class);
		}

	}

}
