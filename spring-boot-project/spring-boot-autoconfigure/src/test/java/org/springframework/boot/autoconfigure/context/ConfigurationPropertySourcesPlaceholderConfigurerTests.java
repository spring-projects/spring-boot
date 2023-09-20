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

package org.springframework.boot.autoconfigure.context;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.MutablePropertySources;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertySourcesPlaceholderConfigurer}.
 *
 * @author Guirong Hu
 */
class ConfigurationPropertySourcesPlaceholderConfigurerTests {

	@Test
	void propertyResolverIsOptimizedForPropertyPlaceholder() {
		ConfigurablePropertyResolver expected = ConfigurationPropertySources
				.createPropertyResolver(new MutablePropertySources());

		new ApplicationContextRunner().withUserConfiguration(GetPropertyResolverPlaceholderConfigurerConfig.class)
				.run((context) -> assertThat(
						context.getBean(GetPropertyResolverPlaceholderConfigurer.class).getPropertyResolver())
								.hasSameClassAs(expected));
	}

	@Configuration(proxyBeanMethods = false)
	static class GetPropertyResolverPlaceholderConfigurerConfig {

		@Bean
		static GetPropertyResolverPlaceholderConfigurer getPropertyResolverPlaceholderConfigurer() {
			return new GetPropertyResolverPlaceholderConfigurer();
		}

	}

	static class GetPropertyResolverPlaceholderConfigurer extends ConfigurationPropertySourcesPlaceholderConfigurer {

		private ConfigurablePropertyResolver propertyResolver;

		@Override
		protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
				ConfigurablePropertyResolver propertyResolver) throws BeansException {
			this.propertyResolver = propertyResolver;
			super.processProperties(beanFactoryToProcess, propertyResolver);
		}

		public ConfigurablePropertyResolver getPropertyResolver() {
			return this.propertyResolver;
		}

	}

}
