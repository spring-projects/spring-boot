/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.admin;

import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.admin.SpringApplicationAdminMXBeanRegistrar;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link SpringApplicationAdminJmxAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
public class SpringApplicationAdminJmxAutoConfigurationTests {

	private static final String ENABLE_ADMIN_PROP = "spring.application.admin.enabled=true";

	private static final String DEFAULT_JMX_NAME = "org.springframework.boot:type=Admin,name=SpringApplication";

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class,
					SpringApplicationAdminJmxAutoConfiguration.class));

	@Test
	public void notRegisteredByDefault()
			throws MalformedObjectNameException, InstanceNotFoundException {
		this.contextRunner.run((context) -> {
			this.thrown.expect(InstanceNotFoundException.class);
			this.server.getObjectInstance(createDefaultObjectName());
		});
	}

	@Test
	public void registeredWithProperty() throws Exception {
		this.contextRunner.withPropertyValues(ENABLE_ADMIN_PROP).run((context) -> {
			ObjectName objectName = createDefaultObjectName();
			ObjectInstance objectInstance = this.server.getObjectInstance(objectName);
			assertThat(objectInstance).as("Lifecycle bean should have been registered")
					.isNotNull();
		});
	}

	@Test
	public void registerWithCustomJmxName() throws InstanceNotFoundException {
		String customJmxName = "org.acme:name=FooBar";
		this.contextRunner
				.withSystemProperties(
						"spring.application.admin.jmx-name=" + customJmxName)
				.withPropertyValues(ENABLE_ADMIN_PROP).run((context) -> {
					try {
						this.server.getObjectInstance(createObjectName(customJmxName));
					}
					catch (InstanceNotFoundException ex) {
						fail("Admin MBean should have been exposed with custom name");
					}
					this.thrown.expect(InstanceNotFoundException.class); // Should not be
																			// exposed
					this.server.getObjectInstance(createDefaultObjectName());
				});
	}

	@Test
	public void registerWithSimpleWebApp() throws Exception {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.sources(ServletWebServerFactoryAutoConfiguration.class,
						DispatcherServletAutoConfiguration.class,
						JmxAutoConfiguration.class,
						SpringApplicationAdminJmxAutoConfiguration.class)
				.run("--" + ENABLE_ADMIN_PROP, "--server.port=0")) {
			assertThat(context).isInstanceOf(ServletWebServerApplicationContext.class);
			assertThat(this.server.getAttribute(createDefaultObjectName(),
					"EmbeddedWebApplication")).isEqualTo(Boolean.TRUE);
			int expected = ((ServletWebServerApplicationContext) context).getWebServer()
					.getPort();
			String actual = getProperty(createDefaultObjectName(), "local.server.port");
			assertThat(actual).isEqualTo(String.valueOf(expected));
		}
	}

	@Test
	public void onlyRegisteredOnceWhenThereIsAChildContext() throws Exception {
		SpringApplicationBuilder parentBuilder = new SpringApplicationBuilder()
				.web(WebApplicationType.NONE).sources(JmxAutoConfiguration.class,
						SpringApplicationAdminJmxAutoConfiguration.class);
		SpringApplicationBuilder childBuilder = parentBuilder
				.child(JmxAutoConfiguration.class,
						SpringApplicationAdminJmxAutoConfiguration.class)
				.web(WebApplicationType.NONE);
		try (ConfigurableApplicationContext parent = parentBuilder
				.run("--" + ENABLE_ADMIN_PROP);
				ConfigurableApplicationContext child = childBuilder
						.run("--" + ENABLE_ADMIN_PROP)) {
			BeanFactoryUtils.beanOfType(parent.getBeanFactory(),
					SpringApplicationAdminMXBeanRegistrar.class);
			this.thrown.expect(NoSuchBeanDefinitionException.class);
			BeanFactoryUtils.beanOfType(child.getBeanFactory(),
					SpringApplicationAdminMXBeanRegistrar.class);
		}
	}

	private ObjectName createDefaultObjectName() {
		return createObjectName(DEFAULT_JMX_NAME);
	}

	private ObjectName createObjectName(String jmxName) {
		try {
			return new ObjectName(jmxName);
		}
		catch (MalformedObjectNameException ex) {
			throw new IllegalStateException("Invalid jmx name " + jmxName, ex);
		}
	}

	private String getProperty(ObjectName objectName, String key) throws Exception {
		return (String) this.server.invoke(objectName, "getProperty",
				new Object[] { key }, new String[] { String.class.getName() });
	}

}
