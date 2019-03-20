/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.List;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2AutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OAuth2ResourceServerConfiguration} when there are multiple
 * {@link ResourceServerConfiguration} beans.
 *
 * @author Dave Syer
 */
public class MultipleResourceServerConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void orderIsUnchangedWhenThereAreMultipleResourceServerConfigurations() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(DoubleResourceConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"security.oauth2.resource.tokenInfoUri:http://example.com",
				"security.oauth2.client.clientId=acme");
		this.context.refresh();
		assertThat(this.context
				.getBean("adminResources", ResourceServerConfiguration.class).getOrder())
						.isEqualTo(3);
		assertThat(this.context
				.getBean("otherResources", ResourceServerConfiguration.class).getOrder())
						.isEqualTo(4);
	}

	@ImportAutoConfiguration({ OAuth2AutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	@EnableWebSecurity
	@Configuration
	protected static class DoubleResourceConfiguration {

		@Bean
		protected ResourceServerConfiguration adminResources() {

			ResourceServerConfiguration resource = new ResourceServerConfiguration() {
				// Switch off the Spring Boot @Autowired configurers
				@Override
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
				@Override
				public void setConfigurers(List<ResourceServerConfigurer> configurers) {
					super.setConfigurers(configurers);
				}
			};
			resource.setOrder(4);
			return resource;
		}

	}

}
