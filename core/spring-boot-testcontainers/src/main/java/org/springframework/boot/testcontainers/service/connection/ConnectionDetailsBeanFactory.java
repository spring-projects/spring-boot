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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.util.Assert;

/**
 * Factory used to create connection details from a container bean at runtime.
 *
 * @author Goutam Adwant
 */
class ConnectionDetailsBeanFactory implements BeanFactoryAware {

	private @Nullable ConfigurableListableBeanFactory beanFactory;

	private @Nullable ConnectionDetailsFactories connectionDetailsFactories;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		ConfigurableListableBeanFactory listableBeanFactory = (ConfigurableListableBeanFactory) beanFactory;
		this.beanFactory = listableBeanFactory;
		this.connectionDetailsFactories = new ConnectionDetailsFactories(listableBeanFactory.getBeanClassLoader());
	}

	ConnectionDetails getConnectionDetails(String beanName, Class<?> connectionDetailsType) {
		ConfigurableListableBeanFactory beanFactory = this.beanFactory;
		Assert.state(beanFactory != null, "BeanFactory has not been set");
		ConnectionDetailsFactories connectionDetailsFactories = this.connectionDetailsFactories;
		Assert.state(connectionDetailsFactories != null, "ConnectionDetailsFactories has not been set");
		for (ContainerConnectionSource<?> source : ServiceConnectionAutoConfigurationRegistrar.getSources(beanFactory,
				beanName)) {
			ConnectionDetails connectionDetails = connectionDetailsFactories.getConnectionDetails(source, true)
				.get(connectionDetailsType);
			if (connectionDetails != null) {
				return connectionDetails;
			}
		}
		throw new IllegalStateException("No connection details of type '%s' found for container bean '%s'"
			.formatted(connectionDetailsType.getName(), beanName));
	}

}
