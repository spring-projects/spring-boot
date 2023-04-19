/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.jersey;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.DefaultJerseyApplicationPath;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JerseySameManagementContextConfiguration}.
 *
 * @author Madhura Bhave
 */
@ClassPathExclusions("spring-webmvc-*")
class JerseySameManagementContextConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JerseySameManagementContextConfiguration.class));

	@Test
	void autoConfigurationIsConditionalOnServletWebApplication() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JerseySameManagementContextConfiguration.class));
		contextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(JerseySameManagementContextConfiguration.class));
	}

	@Test
	void autoConfigurationIsConditionalOnClassResourceConfig() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ResourceConfig.class))
			.run((context) -> assertThat(context).doesNotHaveBean(JerseySameManagementContextConfiguration.class));
	}

	@Test
	void jerseyApplicationPathIsAutoConfiguredWhenNeeded() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(DefaultJerseyApplicationPath.class));
	}

	@Test
	void jerseyApplicationPathIsConditionalOnMissingBean() {
		this.contextRunner.withUserConfiguration(ConfigWithJerseyApplicationPath.class).run((context) -> {
			assertThat(context).hasSingleBean(JerseyApplicationPath.class);
			assertThat(context).hasBean("testJerseyApplicationPath");
		});
	}

	@Test
	void existingResourceConfigBeanShouldNotAutoConfigureRelatedBeans() {
		this.contextRunner.withUserConfiguration(ConfigWithResourceConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(ResourceConfig.class);
			assertThat(context).doesNotHaveBean(JerseyApplicationPath.class);
			assertThat(context).doesNotHaveBean(ServletRegistrationBean.class);
			assertThat(context).hasBean("customResourceConfig");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void servletRegistrationBeanIsAutoConfiguredWhenNeeded() {
		this.contextRunner.withPropertyValues("spring.jersey.application-path=/jersey").run((context) -> {
			ServletRegistrationBean<ServletContainer> bean = context.getBean(ServletRegistrationBean.class);
			assertThat(bean.getUrlMappings()).containsExactly("/jersey/*");
		});
	}

	@Test
	void resourceConfigIsCustomizedWithResourceConfigCustomizerBean() {
		this.contextRunner.withUserConfiguration(CustomizerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(ResourceConfig.class);
			ResourceConfig config = context.getBean(ResourceConfig.class);
			ManagementContextResourceConfigCustomizer customizer = context
				.getBean(ManagementContextResourceConfigCustomizer.class);
			then(customizer).should().customize(config);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigWithJerseyApplicationPath {

		@Bean
		JerseyApplicationPath testJerseyApplicationPath() {
			return mock(JerseyApplicationPath.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigWithResourceConfig {

		@Bean
		ResourceConfig customResourceConfig() {
			return new ResourceConfig();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfiguration {

		@Bean
		ManagementContextResourceConfigCustomizer resourceConfigCustomizer() {
			return mock(ManagementContextResourceConfigCustomizer.class);
		}

	}

}
