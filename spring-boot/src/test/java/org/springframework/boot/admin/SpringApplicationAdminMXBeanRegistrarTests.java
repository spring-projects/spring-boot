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

package org.springframework.boot.admin;

import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link SpringApplicationAdminMXBeanRegistrar}.
 *
 * @author Stephane Nicoll
 */
public class SpringApplicationAdminMXBeanRegistrarTests {

	private static final String OBJECT_NAME = "org.springframework.boot:type=Test,name=SpringApplication";

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private MBeanServer mBeanServer;

	private ConfigurableApplicationContext context;

	@Before
	public void setup() throws MalformedObjectNameException {
		this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
	}

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void validateReadyFlag() {
		final ObjectName objectName = createObjectName(OBJECT_NAME);
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		application.addListeners(new ApplicationListener<ContextRefreshedEvent>() {
			@Override
			public void onApplicationEvent(ContextRefreshedEvent event) {
				try {
					assertThat(isApplicationReady(objectName), is(false));
				}
				catch (Exception ex) {
					throw new IllegalStateException(
							"Could not contact spring application admin bean", ex);
				}
			}
		});
		this.context = application.run();
		assertThat(isApplicationReady(objectName), is(true));
	}

	@Test
	public void environmentIsExposed() {
		final ObjectName objectName = createObjectName(OBJECT_NAME);
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		this.context = application.run("--foo.bar=blam");
		assertThat(isApplicationReady(objectName), is(true));
		assertThat(isApplicationEmbeddedWebApplication(objectName), is(false));
		assertThat(getProperty(objectName, "foo.bar"), is("blam"));
		assertThat(getProperty(objectName, "does.not.exist.test"), is(nullValue()));
	}

	@Test
	public void shutdownApp() throws InstanceNotFoundException {
		final ObjectName objectName = createObjectName(OBJECT_NAME);
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(this.context.isRunning(), is(true));
		invokeShutdown(objectName);
		assertThat(this.context.isRunning(), is(false));
		this.thrown.expect(InstanceNotFoundException.class); // JMX cleanup
		this.mBeanServer.getObjectInstance(objectName);
	}

	private Boolean isApplicationReady(ObjectName objectName) {
		return getAttribute(objectName, Boolean.class, "Ready");
	}

	private Boolean isApplicationEmbeddedWebApplication(ObjectName objectName) {
		return getAttribute(objectName, Boolean.class, "EmbeddedWebApplication");
	}

	private String getProperty(ObjectName objectName, String key) {
		try {
			return (String) this.mBeanServer.invoke(objectName, "getProperty",
					new Object[] { key }, new String[] { String.class.getName() });
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
	}

	private <T> T getAttribute(ObjectName objectName, Class<T> type, String attribute) {
		try {
			Object value = this.mBeanServer.getAttribute(objectName, attribute);
			assertThat((value == null || type.isInstance(value)), is(true));
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
		catch (MalformedObjectNameException e) {
			throw new IllegalStateException("Invalid jmx name " + jmxName, e);
		}
	}

	@Configuration
	static class Config {

		@Bean
		public SpringApplicationAdminMXBeanRegistrar springApplicationAdminRegistrar()
				throws MalformedObjectNameException {
			return new SpringApplicationAdminMXBeanRegistrar(OBJECT_NAME);
		}

	}

}
