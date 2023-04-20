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

package org.springframework.boot.testcontainers.service.connection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.testcontainers.containers.Container;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.boot.origin.Origin;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for {@link ServiceConnection @ServiceConnection} annotated
 * {@link Container} beans.
 *
 * @author Phillip Webb
 * @since 3.1.0
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Import(ServiceConnectionAutoConfiguration.Registrar.class)
public class ServiceConnectionAutoConfiguration {

	ServiceConnectionAutoConfiguration() {
	}

	static class Registrar implements ImportBeanDefinitionRegistrar {

		private final BeanFactory beanFactory;

		Registrar(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			if (this.beanFactory instanceof ConfigurableListableBeanFactory listableBeanFactory) {
				ConnectionDetailsFactories connectionDetailsFactories = new ConnectionDetailsFactories();
				List<ContainerConnectionSource<?>> sources = getSources(listableBeanFactory);
				new ContainerConnectionSourcesRegistrar(listableBeanFactory, connectionDetailsFactories, sources)
					.registerBeanDefinitions(registry);
			}
		}

		private List<ContainerConnectionSource<?>> getSources(ConfigurableListableBeanFactory beanFactory) {
			List<ContainerConnectionSource<?>> sources = new ArrayList<>();
			for (String candidate : beanFactory.getBeanNamesForType(Container.class)) {
				Set<ServiceConnection> annotations = beanFactory.findAllAnnotationsOnBean(candidate,
						ServiceConnection.class, false);
				if (!annotations.isEmpty()) {
					addSources(sources, beanFactory, candidate, annotations);
				}
			}
			return sources;
		}

		private void addSources(List<ContainerConnectionSource<?>> sources, ConfigurableListableBeanFactory beanFactory,
				String beanName, Set<ServiceConnection> annotations) {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			Origin origin = new BeanOrigin(beanName, beanDefinition);
			Container<?> container = beanFactory.getBean(beanName, Container.class);
			for (ServiceConnection annotation : annotations) {
				sources.add(new ContainerConnectionSource<>(beanName, origin, container, annotation));
			}
		}

	}

}
