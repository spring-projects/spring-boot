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

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
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

	private final ConnectionDetailsFactories connectionDetailsFactories;

	ServiceConnectionContextCustomizer(List<ContainerConnectionSource<?>> sources) {
		this(sources, new ConnectionDetailsFactories());
	}

	ServiceConnectionContextCustomizer(List<ContainerConnectionSource<?>> sources,
			ConnectionDetailsFactories connectionDetailsFactories) {
		this.sources = sources;
		this.connectionDetailsFactories = connectionDetailsFactories;
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory instanceof BeanDefinitionRegistry registry) {
			new ContainerConnectionSourcesRegistrar(beanFactory, this.connectionDetailsFactories, this.sources)
				.registerBeanDefinitions(registry);
		}
	}

	List<ContainerConnectionSource<?>> getSources() {
		return this.sources;
	}

}
