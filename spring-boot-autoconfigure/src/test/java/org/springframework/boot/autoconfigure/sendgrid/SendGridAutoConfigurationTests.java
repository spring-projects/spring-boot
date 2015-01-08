/*
 * Copyright 2012-2014 the original author or authors.
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

import com.sendgrid.SendGrid;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "sendgrid.username:user");
		EnvironmentTestUtils.addEnvironment(this.context, "sendgrid.password:secret");
		this.context.register(SendGridAutoConfiguration.class);
		this.context.refresh();

		SendGrid bean = this.context.getBean(SendGrid.class);
		assertNotNull(bean);
	}

	@Test
	public void autoConfigurationNotFiredWhenPropertiesNotSet() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(SendGridAutoConfiguration.class);
		this.context.refresh();

		try {
			this.context.getBean(SendGrid.class);
			fail("Unexpected bean in context of type SendGrid");
		} catch (NoSuchBeanDefinitionException ignored) {
		}
	}

	@Test
	public void autoConfigurationNotFiredWhenBeanAlreadyCreated() {
		final String username = "user";
		final String password = "secret";

		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "sendgrid.username:" + username, "sendgrid.password:" + password);
		this.context.register(SendGridAutoConfiguration.class, ManualSendGridConfiguration.class);
		this.context.refresh();

		SendGrid bean = this.context.getBean(SendGrid.class);

		assertNotEquals(username, ReflectionTestUtils.getField(bean, "username"));
		assertNotEquals(password, ReflectionTestUtils.getField(bean, "password"));
	}

	@Test
	public void expectedSendGridBeanWithProxyCreated() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "sendgrid.username:user");
		EnvironmentTestUtils.addEnvironment(this.context, "sendgrid.password:secret");
		EnvironmentTestUtils.addEnvironment(this.context, "sendgrid.proxy.host:localhost");
		EnvironmentTestUtils.addEnvironment(this.context, "sendgrid.proxy.port:5678");
		this.context.register(SendGridAutoConfiguration.class);
		this.context.refresh();

		SendGrid bean = this.context.getBean(SendGrid.class);
		assertNotNull(bean);
	}

	@Configuration
	static class ManualSendGridConfiguration {

		@Bean
		SendGrid sendGrid() {
			return new SendGrid("manual-user", "manual-secret");
		}
	}
}