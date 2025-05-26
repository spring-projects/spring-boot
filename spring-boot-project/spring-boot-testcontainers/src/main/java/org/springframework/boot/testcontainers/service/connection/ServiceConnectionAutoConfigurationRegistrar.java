/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.LinkedHashSet;
import java.util.Set;

import org.testcontainers.containers.Container;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.testcontainers.beans.TestcontainerBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link ImportBeanDefinitionRegistrar} used by
 * {@link ServiceConnectionAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class ServiceConnectionAutoConfigurationRegistrar implements ImportBeanDefinitionRegistrar {

	private final BeanFactory beanFactory;

	ServiceConnectionAutoConfigurationRegistrar(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (this.beanFactory instanceof ConfigurableListableBeanFactory listableBeanFactory) {
			registerBeanDefinitions(listableBeanFactory, registry);
		}
	}

	private void registerBeanDefinitions(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry) {
		ConnectionDetailsRegistrar registrar = new ConnectionDetailsRegistrar(beanFactory,
				new ConnectionDetailsFactories(null));
		for (String beanName : beanFactory.getBeanNamesForType(Container.class)) {
			BeanDefinition beanDefinition = getBeanDefinition(beanFactory, beanName);
			MergedAnnotations annotations = (beanDefinition instanceof TestcontainerBeanDefinition testcontainerBeanDefinition)
					? testcontainerBeanDefinition.getAnnotations() : null;
			for (ServiceConnection serviceConnection : getServiceConnections(beanFactory, beanName, annotations)) {
				ContainerConnectionSource<?> source = createSource(beanFactory, beanName, beanDefinition, annotations,
						serviceConnection);
				registrar.registerBeanDefinitions(registry, source);
			}
		}
	}

	private Set<ServiceConnection> getServiceConnections(ConfigurableListableBeanFactory beanFactory, String beanName,
			MergedAnnotations annotations) {
		Set<ServiceConnection> serviceConnections = beanFactory.findAllAnnotationsOnBean(beanName,
				ServiceConnection.class, false);
		if (annotations != null) {
			serviceConnections = new LinkedHashSet<>(serviceConnections);
			annotations.stream(ServiceConnection.class)
				.map(MergedAnnotation::synthesize)
				.forEach(serviceConnections::add);
		}
		return serviceConnections;
	}

	private BeanDefinition getBeanDefinition(ConfigurableListableBeanFactory beanFactory, String beanName) {
		try {
			return beanFactory.getBeanDefinition(beanName);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private <C extends Container<?>> ContainerConnectionSource<C> createSource(
			ConfigurableListableBeanFactory beanFactory, String beanName, BeanDefinition beanDefinition,
			MergedAnnotations annotations, ServiceConnection serviceConnection) {
		Origin origin = new BeanOrigin(beanName, beanDefinition);
		Class<C> containerType = (Class<C>) beanFactory.getType(beanName, false);
		String containerImageName = (beanDefinition instanceof TestcontainerBeanDefinition testcontainerBeanDefinition)
				? testcontainerBeanDefinition.getContainerImageName() : null;
		return new ContainerConnectionSource<>(beanName, origin, containerType, containerImageName, serviceConnection,
				() -> beanFactory.getBean(beanName, containerType),
				SslBundleSource.get(beanFactory, beanName, annotations), annotations);
	}

}
