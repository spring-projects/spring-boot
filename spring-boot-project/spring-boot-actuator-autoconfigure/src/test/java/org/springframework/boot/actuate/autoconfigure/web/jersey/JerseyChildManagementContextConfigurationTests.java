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

package org.springframework.boot.actuate.autoconfigure.web.jersey;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JerseyChildManagementContextConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("spring-webmvc-*")
public class JerseyChildManagementContextConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withUserConfiguration(JerseyChildManagementContextConfiguration.class);

	@Test
	public void autoConfigurationIsConditionalOnServletWebApplication() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(JerseySameManagementContextConfiguration.class));
		contextRunner
				.run((context) -> assertThat(context).doesNotHaveBean(JerseySameManagementContextConfiguration.class));
	}

	@Test
	public void autoConfigurationIsConditionalOnClassResourceConfig() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ResourceConfig.class))
				.run((context) -> assertThat(context).doesNotHaveBean(JerseySameManagementContextConfiguration.class));
	}

	@Test
	public void resourceConfigIsCustomizedWithResourceConfigCustomizerBean() {
		this.contextRunner.withUserConfiguration(CustomizerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(ResourceConfig.class);
			ResourceConfig config = context.getBean(ResourceConfig.class);
			ResourceConfigCustomizer customizer = context.getBean(ResourceConfigCustomizer.class);
			verify(customizer).customize(config);
		});
	}

	@Test
	public void jerseyApplicationPathIsAutoConfigured() {
		this.contextRunner.run((context) -> {
			JerseyApplicationPath bean = context.getBean(JerseyApplicationPath.class);
			assertThat(bean.getPath()).isEqualTo("/");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void servletRegistrationBeanIsAutoConfigured() {
		this.contextRunner.run((context) -> {
			ServletRegistrationBean<ServletContainer> bean = context.getBean(ServletRegistrationBean.class);
			assertThat(bean.getUrlMappings()).containsExactly("/*");
		});
	}

	@Test
	public void resourceConfigCustomizerBeanIsNotRequired() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ResourceConfig.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfiguration {

		@Bean
		ResourceConfigCustomizer resourceConfigCustomizer() {
			return mock(ResourceConfigCustomizer.class);
		}

	}

}
