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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.List;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2AutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2ClientProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurer;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Dave Syer
 *
 */
public class MultipleResourceServerConfigurationTests {

	private ConfigurableApplicationContext context;

	private ConfigurableEnvironment environment = new StandardEnvironment();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void doubleResourceServerConfiguration() {
		EnvironmentTestUtils.addEnvironment(this.environment, "debug=true",
				"security.oauth2.resource.tokenInfoUri:http://example.com", "security.oauth2.client.clientId=acme");
		this.context = new SpringApplicationBuilder(DoubleResourceConfiguration.class, MockServletConfiguration.class)
				.environment(this.environment).run();
		RemoteTokenServices services = this.context.getBean(RemoteTokenServices.class);
		assertThat(services).isNotNull();
	}

	@Configuration
	@Import({ OAuth2AutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	@EnableConfigurationProperties(OAuth2ClientProperties.class)
	@EnableWebSecurity
	protected static class MockServletConfiguration {
		@Bean
		public EmbeddedServletContainerFactory embeddedServletContainerFactory() {
			return mock(EmbeddedServletContainerFactory.class);
		}
	}

	@Configuration
	protected static class DoubleResourceConfiguration {

		@Bean
		protected ResourceServerConfiguration adminResources() {

			ResourceServerConfiguration resource = new ResourceServerConfiguration() {
				// Switch off the Spring Boot @Autowired configurers
				public void setConfigurers(List<ResourceServerConfigurer> configurers) {
					super.setConfigurers(configurers);
				}
			};
			resource.setOrder(3);
			return resource;
		}

		@Bean
		protected ResourceServerConfiguration otherResources() {

			ResourceServerConfiguration resource = new ResourceServerConfiguration() {
				// Switch off the Spring Boot @Autowired configurers
				public void setConfigurers(List<ResourceServerConfigurer> configurers) {
					super.setConfigurers(configurers);
				}
			};
			resource.setOrder(4);
			return resource;
		}

	}

}
