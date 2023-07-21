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
import java.util.function.Supplier;

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

	private final Class<C> containerType;

	private final String connectionName;

	private final Set<Class<?>> connectionDetailsTypes;

	private Supplier<C> containerSupplier;

	ContainerConnectionSource(String beanNameSuffix, Origin origin, Class<C> containerType, String containerImageName,
			MergedAnnotation<ServiceConnection> annotation, Supplier<C> containerSupplier) {
		this.beanNameSuffix = beanNameSuffix;
		this.origin = origin;
		this.containerType = containerType;
		this.connectionName = getOrDeduceConnectionName(annotation.getString("name"), containerImageName);
		this.connectionDetailsTypes = Set.of(annotation.getClassArray("type"));
		this.containerSupplier = containerSupplier;
	}

	ContainerConnectionSource(String beanNameSuffix, Origin origin, Class<C> containerType, String containerImageName,
			ServiceConnection annotation, Supplier<C> containerSupplier) {
		this.beanNameSuffix = beanNameSuffix;
		this.origin = origin;
		this.containerType = containerType;
		this.connectionName = getOrDeduceConnectionName(annotation.name(), containerImageName);
		this.connectionDetailsTypes = Set.of(annotation.type());
		this.containerSupplier = containerSupplier;
	}

	private static String getOrDeduceConnectionName(String connectionName, String containerImageName) {
		if (StringUtils.hasText(connectionName)) {
			return connectionName;
		}
		if (StringUtils.hasText(containerImageName)) {
			DockerImageName imageName = DockerImageName.parse(containerImageName);
			imageName.assertValid();
			return imageName.getRepository();
		}
		return null;
	}

	boolean accepts(String requiredConnectionName, Class<?> requiredContainerType,
			Class<?> requiredConnectionDetailsType) {
		if (StringUtils.hasText(requiredConnectionName)
				&& !requiredConnectionName.equalsIgnoreCase(this.connectionName)) {
			logger.trace(LogMessage
				.of(() -> "%s not accepted as source connection name '%s' does not match required connection name '%s'"
					.formatted(this, this.connectionName, requiredConnectionName)));
			return false;
		}
		if (!requiredContainerType.isAssignableFrom(this.containerType)) {
			logger.trace(LogMessage.of(() -> "%s not accepted as source container type %s is not assignable from %s"
				.formatted(this, this.containerType.getName(), requiredContainerType.getName())));
			return false;
		}
		if (!this.connectionDetailsTypes.isEmpty() && this.connectionDetailsTypes.stream()
			.noneMatch((candidate) -> candidate.isAssignableFrom(requiredConnectionDetailsType))) {
			logger.trace(LogMessage
				.of(() -> "%s not accepted as source connection details types %s has no element assignable from %s"
					.formatted(this, this.connectionDetailsTypes.stream().map(Class::getName).toList(),
							requiredConnectionDetailsType.getName())));
			return false;
		}
		logger.trace(
				LogMessage.of(() -> "%s accepted for connection name '%s' container type %s, connection details type %s"
					.formatted(this, requiredConnectionName, requiredContainerType.getName(),
							requiredConnectionDetailsType.getName())));
		return true;
	}

	String getBeanNameSuffix() {
		return this.beanNameSuffix;
	}

	@Override
	public Origin getOrigin() {
		return this.origin;
	}

	String getConnectionName() {
		return this.connectionName;
	}

	Supplier<C> getContainerSupplier() {
		return this.containerSupplier;
	}

	Set<Class<?>> getConnectionDetailsTypes() {
		return this.connectionDetailsTypes;
	}

	@Override
	public String toString() {
		return "@ServiceConnection source for %s".formatted(this.origin);
	}

}
