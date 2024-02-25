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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactoryNotFoundException;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsNotFoundException;
import org.springframework.core.log.LogMessage;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Class used to register {@link ConnectionDetails} bean definitions from
 * {@link ContainerConnectionSource} instances.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ConnectionDetailsRegistrar {

	private static final Log logger = LogFactory.getLog(ConnectionDetailsRegistrar.class);

	private final ListableBeanFactory beanFactory;

	private final ConnectionDetailsFactories connectionDetailsFactories;

	/**
	 * Constructs a new ConnectionDetailsRegistrar with the specified ListableBeanFactory
	 * and ConnectionDetailsFactories.
	 * @param beanFactory the ListableBeanFactory used to register connection details
	 * @param connectionDetailsFactories the ConnectionDetailsFactories used to create
	 * connection details
	 */
	ConnectionDetailsRegistrar(ListableBeanFactory beanFactory, ConnectionDetailsFactories connectionDetailsFactories) {
		this.beanFactory = beanFactory;
		this.connectionDetailsFactories = connectionDetailsFactories;
	}

	/**
	 * Registers the bean definitions for the given collection of container connection
	 * sources in the specified bean definition registry.
	 * @param registry the bean definition registry to register the bean definitions with
	 * @param sources the collection of container connection sources to register the bean
	 * definitions from
	 */
	void registerBeanDefinitions(BeanDefinitionRegistry registry, Collection<ContainerConnectionSource<?>> sources) {
		sources.forEach((source) -> registerBeanDefinitions(registry, source));
	}

	/**
	 * Registers bean definitions based on the provided {@link ContainerConnectionSource}
	 * and {@link BeanDefinitionRegistry}.
	 * @param registry the {@link BeanDefinitionRegistry} to register the bean definitions
	 * with
	 * @param source the {@link ContainerConnectionSource} to retrieve the connection
	 * details from
	 * @throws ConnectionDetailsFactoryNotFoundException if the connection details factory
	 * is not found
	 * @throws ConnectionDetailsNotFoundException if the connection details are not found
	 */
	void registerBeanDefinitions(BeanDefinitionRegistry registry, ContainerConnectionSource<?> source) {
		try {
			this.connectionDetailsFactories.getConnectionDetails(source, true)
				.forEach((connectionDetailsType, connectionDetails) -> registerBeanDefinition(registry, source,
						connectionDetailsType, connectionDetails));
		}
		catch (ConnectionDetailsFactoryNotFoundException ex) {
			rethrowConnectionDetails(source, ex, ConnectionDetailsFactoryNotFoundException::new);
		}
		catch (ConnectionDetailsNotFoundException ex) {
			rethrowConnectionDetails(source, ex, ConnectionDetailsNotFoundException::new);
		}
	}

	/**
	 * Rethrows the connection details with an optional custom exception factory.
	 * @param source the container connection source
	 * @param ex the runtime exception
	 * @param exceptionFactory the exception factory function
	 * @throws RuntimeException if the connection name is not specified
	 * @throws RuntimeException if any other exception occurs
	 */
	private void rethrowConnectionDetails(ContainerConnectionSource<?> source, RuntimeException ex,
			BiFunction<String, Throwable, RuntimeException> exceptionFactory) {
		if (!StringUtils.hasText(source.getConnectionName())) {
			StringBuilder message = new StringBuilder(ex.getMessage());
			message.append((!message.toString().endsWith(".")) ? "." : "");
			message.append(" You may need to add a 'name' to your @ServiceConnection annotation");
			throw exceptionFactory.apply(message.toString(), ex.getCause());
		}
		throw ex;
	}

	/**
	 * Registers a bean definition for the given connection details.
	 * @param registry the bean definition registry
	 * @param source the container connection source
	 * @param connectionDetailsType the type of the connection details
	 * @param connectionDetails the connection details object
	 * @param <T> the type of the connection details
	 */
	@SuppressWarnings("unchecked")
	private <T> void registerBeanDefinition(BeanDefinitionRegistry registry, ContainerConnectionSource<?> source,
			Class<?> connectionDetailsType, ConnectionDetails connectionDetails) {
		String[] existingBeans = this.beanFactory.getBeanNamesForType(connectionDetailsType);
		if (!ObjectUtils.isEmpty(existingBeans)) {
			logger.debug(LogMessage.of(() -> "Skipping registration of %s due to existing beans %s".formatted(source,
					Arrays.asList(existingBeans))));
			return;
		}
		String beanName = getBeanName(source, connectionDetails);
		Class<T> beanType = (Class<T>) connectionDetails.getClass();
		Supplier<T> beanSupplier = () -> (T) connectionDetails;
		logger.debug(LogMessage.of(() -> "Registering '%s' for %s".formatted(beanName, source)));
		RootBeanDefinition beanDefinition = new RootBeanDefinition(beanType, beanSupplier);
		beanDefinition.setAttribute(ServiceConnection.class.getName(), true);
		registry.registerBeanDefinition(beanName, beanDefinition);
	}

	/**
	 * Returns the bean name for the given source and connection details.
	 * @param source the container connection source
	 * @param connectionDetails the connection details
	 * @return the bean name
	 */
	private String getBeanName(ContainerConnectionSource<?> source, ConnectionDetails connectionDetails) {
		List<String> parts = new ArrayList<>();
		parts.add(ClassUtils.getShortNameAsProperty(connectionDetails.getClass()));
		parts.add("for");
		parts.add(source.getBeanNameSuffix());
		return StringUtils.uncapitalize(parts.stream().map(StringUtils::capitalize).collect(Collectors.joining()));
	}

	/**
	 * ServiceConnectionBeanRegistrationExcludeFilter class.
	 */
	class ServiceConnectionBeanRegistrationExcludeFilter implements BeanRegistrationExcludeFilter {

		/**
		 * Determines if a registered bean should be excluded from Ahead-of-Time (AOT)
		 * processing.
		 * @param registeredBean the registered bean to check
		 * @return true if the registered bean should be excluded from AOT processing,
		 * false otherwise
		 */
		@Override
		public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
			return registeredBean.getMergedBeanDefinition().getAttribute(ServiceConnection.class.getName()) != null;
		}

	}

}
