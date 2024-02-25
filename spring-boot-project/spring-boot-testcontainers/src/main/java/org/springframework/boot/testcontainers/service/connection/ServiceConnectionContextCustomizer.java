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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.testcontainers.containers.Container;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Spring Test {@link ContextCustomizer} to support registering {@link ConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ServiceConnectionContextCustomizer implements ContextCustomizer {

	private final List<ContainerConnectionSource<?>> sources;

	private final Set<CacheKey> keys;

	private final ConnectionDetailsFactories connectionDetailsFactories;

	/**
	 * Constructs a new ServiceConnectionContextCustomizer with the specified list of
	 * container connection sources.
	 * @param sources the list of container connection sources
	 */
	ServiceConnectionContextCustomizer(List<ContainerConnectionSource<?>> sources) {
		this(sources, new ConnectionDetailsFactories());
	}

	/**
	 * Constructs a new ServiceConnectionContextCustomizer with the specified list of
	 * ContainerConnectionSource objects and ConnectionDetailsFactories object.
	 * @param sources the list of ContainerConnectionSource objects to be used for the
	 * service connection context customization
	 * @param connectionDetailsFactories the ConnectionDetailsFactories object to be used
	 * for creating connection details
	 */
	ServiceConnectionContextCustomizer(List<ContainerConnectionSource<?>> sources,
			ConnectionDetailsFactories connectionDetailsFactories) {
		this.sources = sources;
		this.keys = sources.stream().map(CacheKey::new).collect(Collectors.toUnmodifiableSet());
		this.connectionDetailsFactories = connectionDetailsFactories;
	}

	/**
	 * Customizes the application context by initializing Testcontainers and registering
	 * connection details beans.
	 * @param context the configurable application context
	 * @param mergedConfig the merged context configuration
	 */
	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		new TestcontainersLifecycleApplicationContextInitializer().initialize(context);
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory instanceof BeanDefinitionRegistry registry) {
			new ConnectionDetailsRegistrar(beanFactory, this.connectionDetailsFactories)
				.registerBeanDefinitions(registry, this.sources);
		}
	}

	/**
	 * Compares this ServiceConnectionContextCustomizer object to the specified object.
	 * The result is true if and only if the argument is not null and is a
	 * ServiceConnectionContextCustomizer object that represents the same keys as this
	 * object.
	 * @param obj the object to compare this ServiceConnectionContextCustomizer against
	 * @return true if the given object represents a ServiceConnectionContextCustomizer
	 * equivalent to this object, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.keys.equals(((ServiceConnectionContextCustomizer) obj).keys);
	}

	/**
	 * Returns the hash code value for this ServiceConnectionContextCustomizer object.
	 * @return the hash code value for this object
	 */
	@Override
	public int hashCode() {
		return this.keys.hashCode();
	}

	/**
	 * Returns the list of ContainerConnectionSource objects.
	 * @return the list of ContainerConnectionSource objects
	 */
	List<ContainerConnectionSource<?>> getSources() {
		return this.sources;
	}

	/**
	 * Relevant details from {@link ContainerConnectionSource} used as a
	 * MergedContextConfiguration cache key.
	 */
	private record CacheKey(String connectionName, Set<Class<?>> connectionDetailsTypes, Container<?> container) {

		/**
		 * Constructs a new CacheKey object based on the provided
		 * ContainerConnectionSource.
		 * @param source the ContainerConnectionSource used to create the CacheKey
		 */
		CacheKey(ContainerConnectionSource<?> source) {
			this(source.getConnectionName(), source.getConnectionDetailsTypes(), source.getContainerSupplier().get());
		}

	}

}
