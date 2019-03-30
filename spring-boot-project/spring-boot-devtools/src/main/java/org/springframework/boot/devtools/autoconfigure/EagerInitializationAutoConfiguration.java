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

package org.springframework.boot.devtools.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to ensure that DevTools' beans are
 * eagerly initialized when the application is otherwise being initialized lazily.
 *
 * @author Andy Wilkinson
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
public class EagerInitializationAutoConfiguration {

	@Bean
	public static AlwaysEagerBeanFactoryPostProcessor alwaysEagerBeanFactoryPostProcessor() {
		return new AlwaysEagerBeanFactoryPostProcessor();
	}

	private static final class AlwaysEagerBeanFactoryPostProcessor
			implements BeanFactoryPostProcessor, Ordered {

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
			for (String name : beanFactory.getBeanDefinitionNames()) {
				BeanDefinition beanDefinition = beanFactory.getBeanDefinition(name);
				String factoryBeanName = beanDefinition.getFactoryBeanName();
				if (factoryBeanName != null && factoryBeanName
						.startsWith("org.springframework.boot.devtools")) {
					beanDefinition.setLazyInit(false);
				}
			}
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE + 1;
		}

	}

}
