/*
 * Copyright 2012-2019 the original author or authors.
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

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpringApplicationAdminMXBeanRegistrar}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class SpringApplicationAdminMXBeanRegistrarTests {

	private static final String OBJECT_NAME = "org.springframework.boot:type=Test,name=SpringApplication";

	private MBeanServer mBeanServer;

	private ConfigurableApplicationContext context;

	@BeforeEach
	void setup() {
		this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
	}

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void validateReadyFlag() {
		final ObjectName objectName = createObjectName(OBJECT_NAME);
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addListeners((ContextRefreshedEvent event) -> {
			try {
				assertThat(isApplicationReady(objectName)).isFalse();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Could not contact spring application admin bean", ex);
			}
		});
		this.context = application.run();
		assertThat(isApplicationReady(objectName)).isTrue();
	}

	@Test
	void eventsFromOtherContextsAreIgnored() throws MalformedObjectNameException {
		SpringApplicationAdminMXBeanRegistrar registrar = new SpringApplicationAdminMXBeanRegistrar(OBJECT_NAME);
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		registrar.setApplicationContext(context);
		registrar.onApplicationReadyEvent(
				new ApplicationReadyEvent(new SpringApplication(), null, mock(ConfigurableApplicationContext.class)));
		assertThat(isApplicationReady(registrar)).isFalse();
		registrar.onApplicationReadyEvent(new ApplicationReadyEvent(new SpringApplication(), null, context));
		assertThat(isApplicationReady(registrar)).isTrue();
	}

	private boolean isApplicationReady(SpringApplicationAdminMXBeanRegistrar registrar) {
		return (Boolean) ReflectionTestUtils.getField(registrar, "ready");
	}

	@Test
	void environmentIsExposed() {
		final ObjectName objectName = createObjectName(OBJECT_NAME);
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--foo.bar=blam");
		assertThat(isApplicationReady(objectName)).isTrue();
		assertThat(isApplicationEmbeddedWebApplication(objectName)).isFalse();
		assertThat(getProperty(objectName, "foo.bar")).isEqualTo("blam");
		assertThat(getProperty(objectName, "does.not.exist.test")).isNull();
	}

	@Test
	void shutdownApp() throws InstanceNotFoundException {
		final ObjectName objectName = createObjectName(OBJECT_NAME);
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context.isRunning()).isTrue();
		invokeShutdown(objectName);
		assertThat(this.context.isRunning()).isFalse();
		// JMX cleanup
		assertThatExceptionOfType(InstanceNotFoundException.class)
				.isThrownBy(() -> this.mBeanServer.getObjectInstance(objectName));
	}

	private Boolean isApplicationReady(ObjectName objectName) {
		return getAttribute(objectName, Boolean.class, "Ready");
	}

	private Boolean isApplicationEmbeddedWebApplication(ObjectName objectName) {
		return getAttribute(objectName, Boolean.class, "EmbeddedWebApplication");
	}

	private String getProperty(ObjectName objectName, String key) {
		try {
			return (String) this.mBeanServer.invoke(objectName, "getProperty", new Object[] { key },
					new String[] { String.class.getName() });
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
	}

	private <T> T getAttribute(ObjectName objectName, Class<T> type, String attribute) {
		try {
			Object value = this.mBeanServer.getAttribute(objectName, attribute);
			assertThat(value == null || type.isInstance(value)).isTrue();
			return type.cast(value);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
	}

	private void invokeShutdown(ObjectName objectName) {
		try {
			this.mBeanServer.invoke(objectName, "shutdown", null, null);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
	}

	private ObjectName createObjectName(String jmxName) {
		try {
			return new ObjectName(jmxName);
		}
		catch (MalformedObjectNameException ex) {
			throw new IllegalStateException("Invalid jmx name " + jmxName, ex);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		SpringApplicationAdminMXBeanRegistrar springApplicationAdminRegistrar() throws MalformedObjectNameException {
			return new SpringApplicationAdminMXBeanRegistrar(OBJECT_NAME);
		}

	}

}
