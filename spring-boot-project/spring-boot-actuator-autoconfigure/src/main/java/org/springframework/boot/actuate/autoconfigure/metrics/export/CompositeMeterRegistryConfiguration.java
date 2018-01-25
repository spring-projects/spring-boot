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

package org.springframework.boot.actuate.autoconfigure.metrics.export;

import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Conditionally build a composite registry out of more than one registry present.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@Configuration
public class CompositeMeterRegistryConfiguration {
	private static final String COMPOSITE_BEAN_NAME = "compositeMeterRegistry";

	@Bean
	public static BeanFactoryPostProcessor createCompositeMeterRegistryIfNecessary() {
		return new CompositeMeterRegistryPostProcessor();
	}

	@Bean
	public ApplicationListener<ContextRefreshedEvent> addRegistriesToComposite() {
		return event -> {
			ApplicationContext context = event.getApplicationContext();

			if (context.containsBean(COMPOSITE_BEAN_NAME)) {
				CompositeMeterRegistry composite = context
						.getBean(COMPOSITE_BEAN_NAME, CompositeMeterRegistry.class);

				context.getBeansOfType(MeterRegistry.class)
						.entrySet()
						.stream()
						.filter(beanByName -> !beanByName.getKey().equals(COMPOSITE_BEAN_NAME))
						.map(Map.Entry::getValue)
						.forEach(composite::add);
			}
		};
	}

	static class CompositeMeterRegistryPostProcessor implements BeanFactoryPostProcessor {
		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			String[] registryBeans = beanFactory.getBeanNamesForType(MeterRegistry.class, true, false);

			// 1. If there are no meter registries configured, we wire an empty composite that effectively no-ops metrics
			// instrumentation throughout the app. Note that in the absence of specific registry implementations, a
			// SimpleMeterRegistry is autoconfigured. This condition will then only occur if the end user has specifically
			// disabled SimpleMeterRegistry and there are no other implementations available.
			//
			// 2. If there are more than one registries configured, we add them as children of a composite meter registry
			// and mark it primary, so that the composite is injected wherever a MeterRegistry is required.
			//
			// 3. If there are more than one registries configured, but one is already marked as primary, we let the
			// primary registry be.
			//
			// 4. If there is only one registry configured, adding the indirection of a composite is not useful, so
			// we just leave it alone.
			if (registryBeans.length == 0 || registryBeans.length > 1) {
				for (String registryBean : registryBeans) {
					if (beanFactory.getBeanDefinition(registryBean).isPrimary()) {
						return;
					}
				}

				GenericBeanDefinition bd = new GenericBeanDefinition();
				bd.setBeanClass(CompositeMeterRegistry.class);
				bd.setPrimary(true);

				((DefaultListableBeanFactory) beanFactory).registerBeanDefinition(COMPOSITE_BEAN_NAME, bd);
			}
		}
	}
}
