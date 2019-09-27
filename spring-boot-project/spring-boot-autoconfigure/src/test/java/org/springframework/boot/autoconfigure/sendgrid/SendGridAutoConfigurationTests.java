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

package org.springframework.boot.autoconfigure.sendgrid;

import com.sendgrid.SendGrid;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link SendGridAutoConfiguration}.
 *
 * @author Maciej Walkowiak
 * @author Patrick Bray
 */
class SendGridAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void expectedSendGridBeanCreatedApiKey() {
		loadContext("spring.sendgrid.api-key:SG.SECRET-API-KEY");
		SendGrid sendGrid = this.context.getBean(SendGrid.class);
		assertThat(sendGrid).extracting("apiKey").isEqualTo("SG.SECRET-API-KEY");
	}

	@Test
	void autoConfigurationNotFiredWhenPropertiesNotSet() {
		loadContext();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.context.getBean(SendGrid.class));
	}

	@Test
	void autoConfigurationNotFiredWhenBeanAlreadyCreated() {
		loadContext(ManualSendGridConfiguration.class, "spring.sendgrid.api-key:SG.SECRET-API-KEY");
		SendGrid sendGrid = this.context.getBean(SendGrid.class);
		assertThat(sendGrid).extracting("apiKey").isEqualTo("SG.CUSTOM_API_KEY");
	}

	@Test
	void expectedSendGridBeanWithProxyCreated() {
		loadContext("spring.sendgrid.api-key:SG.SECRET-API-KEY", "spring.sendgrid.proxy.host:localhost",
				"spring.sendgrid.proxy.port:5678");
		SendGrid sendGrid = this.context.getBean(SendGrid.class);
		assertThat(sendGrid).extracting("client").extracting("httpClient").extracting("routePlanner")
				.isInstanceOf(DefaultProxyRoutePlanner.class);
	}

	private void loadContext(String... environment) {
		this.loadContext(null, environment);
	}

	private void loadContext(Class<?> additionalConfiguration, String... environment) {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(this.context);
		ConfigurationPropertySources.attach(this.context.getEnvironment());
		this.context.register(SendGridAutoConfiguration.class);
		if (additionalConfiguration != null) {
			this.context.register(additionalConfiguration);
		}
		this.context.refresh();
	}

	@Configuration(proxyBeanMethods = false)
	static class ManualSendGridConfiguration {

		@Bean
		SendGrid sendGrid() {
			return new SendGrid("SG.CUSTOM_API_KEY", true);
		}

	}

}
