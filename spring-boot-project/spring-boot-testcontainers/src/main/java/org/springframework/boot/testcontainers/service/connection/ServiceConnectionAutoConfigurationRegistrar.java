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
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link ImportBeanDefinitionRegistrar} used by
 * {@link ServiceConnectionAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class ServiceConnectionAutoConfigurationRegistrar implements ImportBeanDefinitionRegistrar {

	private final BeanFactory beanFactory;

	/**
	 * Constructs a new instance of ServiceConnectionAutoConfigurationRegistrar with the
	 * specified bean factory.
	 * @param beanFactory the bean factory to be used for creating instances of
	 * ServiceConnectionAutoConfigurationRegistrar
	 */
	ServiceConnectionAutoConfigurationRegistrar(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Register the bean definitions for the ServiceConnectionAutoConfigurationRegistrar.
	 * @param importingClassMetadata the metadata of the importing class
	 * @param registry the bean definition registry
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (this.beanFactory instanceof ConfigurableListableBeanFactory listableBeanFactory) {
			registerBeanDefinitions(listableBeanFactory, registry);
		}
	}

	/**
	 * Registers bean definitions for ServiceConnectionAutoConfigurationRegistrar.
	 * @param beanFactory the ConfigurableListableBeanFactory to register bean definitions
	 * with
	 * @param registry the BeanDefinitionRegistry to register bean definitions with
	 */
	private void registerBeanDefinitions(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry) {
		ConnectionDetailsRegistrar registrar = new ConnectionDetailsRegistrar(beanFactory,
				new ConnectionDetailsFactories());
		for (String beanName : beanFactory.getBeanNamesForType(Container.class)) {
			BeanDefinition beanDefinition = getBeanDefinition(beanFactory, beanName);
			for (ServiceConnection annotation : getAnnotations(beanFactory, beanName, beanDefinition)) {
				ContainerConnectionSource<?> source = createSource(beanFactory, beanName, beanDefinition, annotation);
				registrar.registerBeanDefinitions(registry, source);
			}
		}
	}

	/**
	 * Retrieves the annotations of type {@link ServiceConnection} for a given bean.
	 * @param beanFactory the bean factory to search for annotations
	 * @param beanName the name of the bean
	 * @param beanDefinition the definition of the bean
	 * @return a set of {@link ServiceConnection} annotations found on the bean
	 */
	private Set<ServiceConnection> getAnnotations(ConfigurableListableBeanFactory beanFactory, String beanName,
			BeanDefinition beanDefinition) {
		Set<ServiceConnection> annotations = new LinkedHashSet<>();
		annotations.addAll(beanFactory.findAllAnnotationsOnBean(beanName, ServiceConnection.class, false));
		if (beanDefinition instanceof TestcontainerBeanDefinition testcontainerBeanDefinition) {
			testcontainerBeanDefinition.getAnnotations()
				.stream(ServiceConnection.class)
				.map(MergedAnnotation::synthesize)
				.forEach(annotations::add);
		}
		return annotations;
	}

	/**
	 * Retrieves the bean definition for the specified bean name from the given bean
	 * factory.
	 * @param beanFactory the configurable listable bean factory to retrieve the bean
	 * definition from
	 * @param beanName the name of the bean to retrieve the definition for
	 * @return the bean definition for the specified bean name, or null if the bean
	 * definition does not exist
	 */
	private BeanDefinition getBeanDefinition(ConfigurableListableBeanFactory beanFactory, String beanName) {
		try {
			return beanFactory.getBeanDefinition(beanName);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

	/**
	 * Creates a {@link ContainerConnectionSource} for the specified bean.
	 * @param beanFactory the bean factory
	 * @param beanName the name of the bean
	 * @param beanDefinition the bean definition
	 * @param annotation the service connection annotation
	 * @param <C> the type of the container
	 * @return the created {@link ContainerConnectionSource}
	 */
	@SuppressWarnings("unchecked")
	private <C extends Container<?>> ContainerConnectionSource<C> createSource(
			ConfigurableListableBeanFactory beanFactory, String beanName, BeanDefinition beanDefinition,
			ServiceConnection annotation) {
		Origin origin = new BeanOrigin(beanName, beanDefinition);
		Class<C> containerType = (Class<C>) beanFactory.getType(beanName, false);
		String containerImageName = (beanDefinition instanceof TestcontainerBeanDefinition testcontainerBeanDefinition)
				? testcontainerBeanDefinition.getContainerImageName() : null;
		return new ContainerConnectionSource<>(beanName, origin, containerType, containerImageName, annotation,
				() -> beanFactory.getBean(beanName, containerType));
	}

}
