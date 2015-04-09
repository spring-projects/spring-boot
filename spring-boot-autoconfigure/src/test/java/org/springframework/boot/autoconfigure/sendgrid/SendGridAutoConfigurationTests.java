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

package org.springframework.boot.autoconfigure.sendgrid;

import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import com.sendgrid.SendGrid;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link SendGridAutoConfiguration}.
 *
 * @author Maciej Walkowiak
 */
public class SendGridAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void expectedSendGridBeanCreated() {
		loadContext("spring.sendgrid.username:user", "spring.sendgrid.password:secret");
		SendGrid sendGrid = this.context.getBean(SendGrid.class);
		assertEquals("user", ReflectionTestUtils.getField(sendGrid, "username"));
		assertEquals("secret", ReflectionTestUtils.getField(sendGrid, "password"));
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void autoConfigurationNotFiredWhenPropertiesNotSet() {
		loadContext();
		this.context.getBean(SendGrid.class);
	}

	@Test
	public void autoConfigurationNotFiredWhenBeanAlreadyCreated() {
		loadContext(ManualSendGridConfiguration.class, "spring.sendgrid.username:user",
				"spring.sendgrid.password:secret");
		SendGrid sendGrid = this.context.getBean(SendGrid.class);
		assertEquals("manual-user", ReflectionTestUtils.getField(sendGrid, "username"));
		assertEquals("manual-secret", ReflectionTestUtils.getField(sendGrid, "password"));
	}

	@Test
	public void expectedSendGridBeanWithProxyCreated() {
		loadContext("spring.sendgrid.username:user", "spring.sendgrid.password:secret",
				"spring.sendgrid.proxy.host:localhost", "spring.sendgrid.proxy.port:5678");
		SendGrid sendGrid = this.context.getBean(SendGrid.class);
		CloseableHttpClient client = (CloseableHttpClient) ReflectionTestUtils.getField(
				sendGrid, "client");
		HttpRoutePlanner routePlanner = (HttpRoutePlanner) ReflectionTestUtils.getField(
				client, "routePlanner");
		assertThat(routePlanner, instanceOf(DefaultProxyRoutePlanner.class));
	}

	private void loadContext(String... environment) {
		this.loadContext(null, environment);
	}

	private void loadContext(Class<?> additionalConfiguration, String... environment) {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, environment);
		this.context.register(SendGridAutoConfiguration.class);
		if (additionalConfiguration != null) {
			this.context.register(additionalConfiguration);
		}
		this.context.refresh();
	}

	@Configuration
	static class ManualSendGridConfiguration {

		@Bean
		SendGrid sendGrid() {
			return new SendGrid("manual-user", "manual-secret");
		}

	}

}
