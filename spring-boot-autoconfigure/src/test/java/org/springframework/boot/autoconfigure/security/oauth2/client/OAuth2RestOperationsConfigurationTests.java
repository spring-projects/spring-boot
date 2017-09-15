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

package org.springframework.boot.autoconfigure.security.oauth2.client;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.web.servlet.MockServletWebServerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.OAuth2ClientConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OAuth2RestOperationsConfiguration}.
 *
 * @author Madhura Bhave
 */
public class OAuth2RestOperationsConfigurationTests {

	private ConfigurableApplicationContext context;

	private ConfigurableEnvironment environment = new StandardEnvironment();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void clientCredentialsWithClientId() throws Exception {
		TestPropertyValues.of("security.oauth2.client.client-id=acme")
				.applyTo(this.environment);
		initializeContext(OAuth2RestOperationsConfiguration.class, true);
		assertThat(this.context.getBean(OAuth2RestOperationsConfiguration.class))
				.isNotNull();
		assertThat(this.context.getBean(ClientCredentialsResourceDetails.class))
				.isNotNull();
	}

	@Test
	public void clientCredentialsWithNoClientId() throws Exception {
		initializeContext(OAuth2RestOperationsConfiguration.class, true);
		assertThat(this.context.getBean(OAuth2RestOperationsConfiguration.class))
				.isNotNull();
		assertThat(this.context.getBean(ClientCredentialsResourceDetails.class))
				.isNotNull();
	}

	@Test
	public void requestScopedWithClientId() throws Exception {
		TestPropertyValues.of("security.oauth2.client.client-id=acme")
				.applyTo(this.environment);
		initializeContext(ConfigForRequestScopedConfiguration.class, false);
		assertThat(this.context.containsBean("oauth2ClientContext")).isTrue();
	}

	@Test
	public void requestScopedWithNoClientId() throws Exception {
		initializeContext(ConfigForRequestScopedConfiguration.class, false);
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(DefaultOAuth2ClientContext.class);
	}

	@Test
	public void sessionScopedWithClientId() throws Exception {
		TestPropertyValues.of("security.oauth2.client.client-id=acme")
				.applyTo(this.environment);
		initializeContext(ConfigForSessionScopedConfiguration.class, false);
		assertThat(this.context.containsBean("oauth2ClientContext")).isTrue();
	}

	@Test
	public void sessionScopedWithNoClientId() throws Exception {
		initializeContext(ConfigForSessionScopedConfiguration.class, false);
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(DefaultOAuth2ClientContext.class);
	}

	private void initializeContext(Class<?> configuration, boolean clientCredentials) {
		this.context = new SpringApplicationBuilder(configuration)
				.environment(this.environment).web(clientCredentials
						? WebApplicationType.NONE : WebApplicationType.SERVLET)
				.run();
	}

	@Configuration
	@Import({ OAuth2RestOperationsConfiguration.class })
	protected static class WebApplicationConfiguration {

		@Bean
		public MockServletWebServerFactory webServerFactory() {
			return new MockServletWebServerFactory();
		}

	}

	@Configuration
	@Import({ SecurityProperties.class, OAuth2ClientConfiguration.class,
			OAuth2RestOperationsConfiguration.class })
	protected static class ConfigForSessionScopedConfiguration
			extends WebApplicationConfiguration {

	}

	@Configuration
	protected static class ConfigForRequestScopedConfiguration
			extends WebApplicationConfiguration {

	}

}
