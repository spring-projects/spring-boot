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
import java.util.function.Supplier;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;

/**
 * {@link ContextCustomizer} to support registering {@link ConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ServiceConnectionContextCustomizer implements ContextCustomizer {

	private final ConnectionDetailsFactories factories = new ConnectionDetailsFactories();

	private final List<ContainerConnectionSource<?, ?, ?>> sources;

	ServiceConnectionContextCustomizer(List<ContainerConnectionSource<?, ?, ?>> sources) {
		this.sources = sources;
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory instanceof BeanDefinitionRegistry registry) {
			registerServiceConnections(registry);
		}
	}

	private void registerServiceConnections(BeanDefinitionRegistry registry) {
		this.sources.forEach((source) -> registerServiceConnection(registry, source));
	}

	private void registerServiceConnection(BeanDefinitionRegistry registry, ContainerConnectionSource<?, ?, ?> source) {
		ConnectionDetails connectionDetails = getConnectionDetails(source);
		register(connectionDetails, registry, source.getBeanName());
	}

	@SuppressWarnings("unchecked")
	private <T> void register(ConnectionDetails connectionDetails, BeanDefinitionRegistry registry, String beanName) {
		Class<T> beanType = (Class<T>) connectionDetails.getClass();
		Supplier<T> beanSupplier = () -> (T) connectionDetails;
		BeanDefinition beanDefinition = new RootBeanDefinition(beanType, beanSupplier);
		registry.registerBeanDefinition(beanName, beanDefinition);
	}

	private <S> ConnectionDetails getConnectionDetails(S source) {
		ConnectionDetails connectionDetails = this.factories.getConnectionDetails(source);
		Assert.state(connectionDetails != null, () -> "No connection details created for %s".formatted(source));
		return connectionDetails;
	}

	List<ContainerConnectionSource<?, ?, ?>> getSources() {
		return this.sources;
	}

}
