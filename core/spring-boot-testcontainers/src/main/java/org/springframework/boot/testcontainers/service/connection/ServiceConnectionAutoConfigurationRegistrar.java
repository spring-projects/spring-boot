/*
 * Copyright 2012-present the original author or authors.
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.testcontainers.containers.Container;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.testcontainers.beans.TestcontainerBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.Assert;

/**
 * {@link ImportBeanDefinitionRegistrar} used by
 * {@link ServiceConnectionAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Daeho Kwon
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
			for (ContainerConnectionSource<?> source : getSources(beanFactory, beanName)) {
				registrar.registerBeanDefinitions(registry, source, (connectionDetailsType,
						connectionDetails) -> createBeanDefinition(beanName, connectionDetailsType));
			}
		}
	}

	/**
	 * Create a bean definition that obtains the {@link ConnectionDetails} from the
	 * container bean when it is instantiated. Unlike a definition with an instance
	 * supplier, it can be processed ahead-of-time so the same path is used at AOT runtime
	 * where this registrar does not run again.
	 * @param containerBeanName the name of the container bean
	 * @param connectionDetailsType the connection details type
	 * @return the bean definition
	 */
	private RootBeanDefinition createBeanDefinition(String containerBeanName, Class<?> connectionDetailsType) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(ServiceConnectionAutoConfigurationRegistrar.class);
		beanDefinition.setTargetType(connectionDetailsType);
		beanDefinition.setFactoryMethodName("getConnectionDetails");
		beanDefinition.setAutowireMode(AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
		beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1, containerBeanName);
		beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(2, connectionDetailsType);
		return beanDefinition;
	}

	static ConnectionDetails getConnectionDetails(ConfigurableListableBeanFactory beanFactory, String containerBeanName,
			Class<?> connectionDetailsType) {
		ConnectionDetailsFactories connectionDetailsFactories = new ConnectionDetailsFactories(null);
		for (ContainerConnectionSource<?> source : getSources(beanFactory, containerBeanName)) {
			ConnectionDetails connectionDetails = connectionDetailsFactories.getConnectionDetails(source, false)
				.get(connectionDetailsType);
			if (connectionDetails != null) {
				return connectionDetails;
			}
		}
		throw new IllegalStateException(
				"No %s found for container bean '%s'".formatted(connectionDetailsType.getName(), containerBeanName));
	}

	private static List<ContainerConnectionSource<?>> getSources(ConfigurableListableBeanFactory beanFactory,
			String beanName) {
		BeanDefinition beanDefinition = getBeanDefinition(beanFactory, beanName);
		MergedAnnotations annotations = getAnnotations(beanDefinition);
		List<ContainerConnectionSource<?>> sources = new ArrayList<>();
		for (ServiceConnection serviceConnection : getServiceConnections(beanFactory, beanName, annotations)) {
			sources.add(createSource(beanFactory, beanName, beanDefinition, annotations, serviceConnection));
		}
		return sources;
	}

	private static Set<ServiceConnection> getServiceConnections(ConfigurableListableBeanFactory beanFactory,
			String beanName, @Nullable MergedAnnotations annotations) {
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

	private static @Nullable BeanDefinition getBeanDefinition(ConfigurableListableBeanFactory beanFactory,
			String beanName) {
		try {
			return beanFactory.getBeanDefinition(beanName);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

	private static @Nullable MergedAnnotations getAnnotations(@Nullable BeanDefinition beanDefinition) {
		if (beanDefinition instanceof TestcontainerBeanDefinition testcontainerBeanDefinition) {
			return testcontainerBeanDefinition.getAnnotations();
		}
		if (beanDefinition instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
			MethodMetadata metadata = annotatedBeanDefinition.getFactoryMethodMetadata();
			return (metadata != null) ? metadata.getAnnotations() : null;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <C extends Container<?>> ContainerConnectionSource<C> createSource(
			ConfigurableListableBeanFactory beanFactory, String beanName, @Nullable BeanDefinition beanDefinition,
			@Nullable MergedAnnotations annotations, ServiceConnection serviceConnection) {
		Origin origin = new BeanOrigin(beanName, beanDefinition);
		Class<C> containerType = (Class<C>) beanFactory.getType(beanName, false);
		String containerImageName = (beanDefinition instanceof TestcontainerBeanDefinition testcontainerBeanDefinition)
				? testcontainerBeanDefinition.getContainerImageName() : null;
		Assert.state(containerType != null, "'containerType' must not be null");
		return new ContainerConnectionSource<>(beanName, origin, containerType, containerImageName, serviceConnection,
				() -> beanFactory.getBean(beanName, containerType),
				SslBundleSource.get(beanFactory, beanName, annotations), annotations);
	}

}
