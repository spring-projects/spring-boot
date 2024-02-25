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

package org.springframework.boot.docker.compose.service.connection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.lifecycle.DockerComposeServicesReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationListener} that listens for an {@link DockerComposeServicesReadyEvent}
 * in order to establish service connections.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerComposeServiceConnectionsApplicationListener
		implements ApplicationListener<DockerComposeServicesReadyEvent> {

	private final ConnectionDetailsFactories factories;

	/**
	 * Constructs a new DockerComposeServiceConnectionsApplicationListener with the
	 * default ConnectionDetailsFactories.
	 */
	DockerComposeServiceConnectionsApplicationListener() {
		this(new ConnectionDetailsFactories());
	}

	/**
	 * Constructs a new DockerComposeServiceConnectionsApplicationListener with the
	 * specified ConnectionDetailsFactories.
	 * @param factories the ConnectionDetailsFactories to be used by the listener
	 */
	DockerComposeServiceConnectionsApplicationListener(ConnectionDetailsFactories factories) {
		this.factories = factories;
	}

	/**
	 * This method is called when the DockerComposeServicesReadyEvent is triggered. It
	 * registers the connection details for the running services in the application
	 * context.
	 * @param event The DockerComposeServicesReadyEvent that triggered this method.
	 * Contains the application context and the list of running services.
	 */
	@Override
	public void onApplicationEvent(DockerComposeServicesReadyEvent event) {
		ApplicationContext applicationContext = event.getSource();
		if (applicationContext instanceof BeanDefinitionRegistry registry) {
			registerConnectionDetails(registry, event.getRunningServices());
		}
	}

	/**
	 * Registers the connection details for the running services.
	 * @param registry the BeanDefinitionRegistry to register the connection details
	 * @param runningServices the list of RunningService objects containing the details of
	 * the running services
	 */
	private void registerConnectionDetails(BeanDefinitionRegistry registry, List<RunningService> runningServices) {
		for (RunningService runningService : runningServices) {
			DockerComposeConnectionSource source = new DockerComposeConnectionSource(runningService);
			this.factories.getConnectionDetails(source, false).forEach((connectionDetailsType, connectionDetails) -> {
				register(registry, runningService, connectionDetailsType, connectionDetails);
				this.factories.getConnectionDetails(connectionDetails, false)
					.forEach((adaptedType, adaptedDetails) -> register(registry, runningService, adaptedType,
							adaptedDetails));
			});
		}
	}

	/**
	 * Registers a bean definition in the given registry for the specified running service
	 * and connection details.
	 * @param registry the bean definition registry to register the bean definition in
	 * @param runningService the running service to register the bean for
	 * @param connectionDetailsType the type of the connection details
	 * @param connectionDetails the connection details to register the bean with
	 * @param <T> the type of the connection details
	 */
	@SuppressWarnings("unchecked")
	private <T> void register(BeanDefinitionRegistry registry, RunningService runningService,
			Class<?> connectionDetailsType, ConnectionDetails connectionDetails) {
		String beanName = getBeanName(runningService, connectionDetailsType);
		Class<T> beanType = (Class<T>) connectionDetails.getClass();
		Supplier<T> beanSupplier = () -> (T) connectionDetails;
		registry.registerBeanDefinition(beanName, new RootBeanDefinition(beanType, beanSupplier));
	}

	/**
	 * Returns the bean name for the given RunningService and connectionDetailsType. The
	 * bean name is generated by concatenating the short name of the
	 * connectionDetailsType, the word "for", and the individual parts of the
	 * RunningService name (split by "-"). The generated bean name is then converted to
	 * lowercase.
	 * @param runningService the RunningService for which the bean name is generated
	 * @param connectionDetailsType the Class representing the connection details type
	 * @return the generated bean name
	 */
	private String getBeanName(RunningService runningService, Class<?> connectionDetailsType) {
		List<String> parts = new ArrayList<>();
		parts.add(ClassUtils.getShortNameAsProperty(connectionDetailsType));
		parts.add("for");
		parts.addAll(Arrays.asList(runningService.name().split("-")));
		return StringUtils.uncapitalize(parts.stream().map(StringUtils::capitalize).collect(Collectors.joining()));
	}

}
