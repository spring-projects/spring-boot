/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.jersey;

import java.util.Collections;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.DefaultJerseyApplicationPath;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JerseyWebEndpointManagementContextConfiguration}.
 *
 * @author Michael Simons
 * @author Madhura Bhave
 */
public class JerseyWebEndpointManagementContextConfigurationTests {

	private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(WebEndpointAutoConfiguration.class,
					JerseyWebEndpointManagementContextConfiguration.class))
			.withUserConfiguration(WebEndpointsSupplierConfig.class);

	@Test
	public void resourceConfigIsAutoConfiguredWhenNeeded() {
		this.runner.run(
				(context) -> assertThat(context).hasSingleBean(ResourceConfig.class));
	}

	@Test
	public void jerseyApplicationPathIsAutoConfiguredWhenNeeded() {
		this.runner.run((context) -> assertThat(context)
				.hasSingleBean(DefaultJerseyApplicationPath.class));
	}

	@Test
	public void jerseyApplicationPathIsConditionalOnMissinBean() {
		this.runner.withUserConfiguration(ConfigWithJerseyApplicationPath.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(JerseyApplicationPath.class);
					assertThat(context).hasBean("testJerseyApplicationPath");
				});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void servletRegistrationBeanIsAutoConfiguredWhenNeeded() {
		this.runner.withPropertyValues("spring.jersey.application-path=/jersey")
				.run((context) -> {
					ServletRegistrationBean<ServletContainer> bean = context
							.getBean(ServletRegistrationBean.class);
					assertThat(bean.getUrlMappings()).containsExactly("/jersey/*");
				});
	}

	@Test
	public void existingResourceConfigBeanShouldNotAutoConfigureRelatedBeans() {
		this.runner.withUserConfiguration(ConfigWithResourceConfig.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(ResourceConfig.class);
					assertThat(context).doesNotHaveBean(JerseyApplicationPath.class);
					assertThat(context).doesNotHaveBean(ServletRegistrationBean.class);
					assertThat(context).hasBean("customResourceConfig");
				});
	}

	@Test
	public void resourceConfigIsCustomizedWithResourceConfigCustomizerBean() {
		this.runner.withUserConfiguration(CustomizerConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(ResourceConfig.class);
					ResourceConfig config = context.getBean(ResourceConfig.class);
					ResourceConfigCustomizer customizer = (ResourceConfigCustomizer) context
							.getBean("testResourceConfigCustomizer");
					verify(customizer).customize(config);
				});
	}

	@Test
	public void resourceConfigCustomizerBeanIsNotRequired() {
		this.runner.run(
				(context) -> assertThat(context).hasSingleBean(ResourceConfig.class));
	}

	@Configuration
	static class CustomizerConfiguration {

		@Bean
		ResourceConfigCustomizer testResourceConfigCustomizer() {
			return mock(ResourceConfigCustomizer.class);
		}

	}

	@Configuration
	static class WebEndpointsSupplierConfig {

		@Bean
		public WebEndpointsSupplier webEndpointsSupplier() {
			return () -> Collections.emptyList();
		}

	}

	@Configuration
	static class ConfigWithResourceConfig {

		@Bean
		public ResourceConfig customResourceConfig() {
			return new ResourceConfig();
		}

	}

	@Configuration
	static class ConfigWithJerseyApplicationPath {

		@Bean
		public JerseyApplicationPath testJerseyApplicationPath() {
			return mock(JerseyApplicationPath.class);
		}

	}

}
