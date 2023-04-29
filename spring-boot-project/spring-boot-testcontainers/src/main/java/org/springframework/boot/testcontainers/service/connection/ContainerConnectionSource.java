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

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.log.LogMessage;
import org.springframework.util.StringUtils;

/**
 * Passed to {@link ContainerConnectionDetailsFactory} to provide details of the
 * {@link ServiceConnection @ServiceConnection} annotated {@link Container} that provides
 * the service.
 *
 * @param <C> the generic container type
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 * @see ContainerConnectionDetailsFactory
 */
public final class ContainerConnectionSource<C extends Container<?>> implements OriginProvider {

	private static final Log logger = LogFactory.getLog(ContainerConnectionSource.class);

	private final String beanNameSuffix;

	private final Origin origin;

	private final C container;

	private final String acceptedConnectionName;

	private final Set<Class<?>> acceptedConnectionDetailsTypes;

	ContainerConnectionSource(String beanNameSuffix, Origin origin, C container,
			MergedAnnotation<ServiceConnection> annotation) {
		this.beanNameSuffix = beanNameSuffix;
		this.origin = origin;
		this.container = container;
		this.acceptedConnectionName = getConnectionName(container, annotation.getString("name"));
		this.acceptedConnectionDetailsTypes = Set.of(annotation.getClassArray("type"));
	}

	ContainerConnectionSource(String beanNameSuffix, Origin origin, C container, ServiceConnection annotation) {
		this.beanNameSuffix = beanNameSuffix;
		this.origin = origin;
		this.container = container;
		this.acceptedConnectionName = getConnectionName(container, annotation.name());
		this.acceptedConnectionDetailsTypes = Set.of(annotation.type());
	}

	private static String getConnectionName(Container<?> container, String connectionName) {
		if (StringUtils.hasLength(connectionName)) {
			return connectionName;
		}
		try {
			DockerImageName imageName = DockerImageName.parse(container.getDockerImageName());
			imageName.assertValid();
			return imageName.getRepository();
		}
		catch (IllegalArgumentException ex) {
			return container.getDockerImageName();
		}
	}

	boolean accepts(String connectionName, Class<?> connectionDetailsType, Class<?> containerType) {
		if (!containerType.isInstance(this.container)) {
			logger.trace(LogMessage.of(() -> "%s not accepted as %s is not an instance of %s".formatted(this,
					this.container.getClass().getName(), containerType.getName())));
			return false;
		}
		if (StringUtils.hasLength(connectionName) && !connectionName.equalsIgnoreCase(this.acceptedConnectionName)) {
			logger.trace(LogMessage.of(() -> "%s not accepted as connection names '%s' and '%s' do not match"
				.formatted(this, connectionName, this.acceptedConnectionName)));
			return false;
		}
		if (!this.acceptedConnectionDetailsTypes.isEmpty() && this.acceptedConnectionDetailsTypes.stream()
			.noneMatch((candidate) -> candidate.isAssignableFrom(connectionDetailsType))) {
			logger.trace(LogMessage.of(() -> "%s not accepted as connection details type %s not in %s".formatted(this,
					connectionDetailsType, this.acceptedConnectionDetailsTypes)));
			return false;
		}
		logger.trace(LogMessage
			.of(() -> "%s accepted for connection name '%s', connection details type %s, container type %s"
				.formatted(this, connectionName, connectionDetailsType.getName(), containerType.getName())));
		return true;
	}

	String getBeanNameSuffix() {
		return this.beanNameSuffix;
	}

	@Override
	public Origin getOrigin() {
		return this.origin;
	}

	C getContainer() {
		return this.container;
	}

	@Override
	public String toString() {
		return "@ServiceConnection source for %s".formatted(this.origin);
	}

}
