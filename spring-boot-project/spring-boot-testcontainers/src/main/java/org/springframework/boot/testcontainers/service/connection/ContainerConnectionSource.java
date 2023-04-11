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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.core.annotation.MergedAnnotation;

/**
 * Passed to {@link ContainerConnectionDetailsFactory} to provide details of the
 * {@link ServiceConnection @ServiceConnection} annotation {@link GenericContainer} field
 * that provides the service.
 *
 * @param <A> the source annotation type. The annotation will mergable to a
 * {@link ServiceConnection @ServiceConnection}
 * @param <D> the connection details type
 * @param <C> the generic container type
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 * @see ContainerConnectionDetailsFactory
 */
public final class ContainerConnectionSource<A extends Annotation, D extends ConnectionDetails, C extends GenericContainer<?>>
		implements OriginProvider {

	private final Class<D> connectionDetailsType;

	private final Field field;

	private final A annotation;

	private final C container;

	private final AnnotatedFieldOrigin origin;

	@SuppressWarnings("unchecked")
	ContainerConnectionSource(Class<D> connectionDetailsType, Field field,
			MergedAnnotation<ServiceConnection> annotation, C container) {
		this(connectionDetailsType, field, (A) annotation.getRoot().synthesize(), container);
	}

	ContainerConnectionSource(Class<D> connectionDetailsType, Field field, A annotation, C container) {
		this.connectionDetailsType = connectionDetailsType;
		this.field = field;
		this.annotation = annotation;
		this.container = container;
		this.origin = new AnnotatedFieldOrigin(field, annotation);
	}

	boolean accepts(Class<?> annotationType, Class<?> connectionDetailsType, Class<?> containerType) {
		return annotationType.isInstance(this.annotation)
				&& connectionDetailsType.isAssignableFrom(this.connectionDetailsType)
				&& containerType.isInstance(this.container);
	}

	String getBeanName() {
		return this.field.getName() + this.connectionDetailsType.getSimpleName() + "ConnectedContainer";
	}

	/**
	 * Return the source annotation that provided the connection to the container. This
	 * annotation will be mergable to {@link ServiceConnection @ServiceConnection}.
	 * @return the source annotation
	 */
	public A getAnnotation() {
		return this.annotation;
	}

	/**
	 * Return the {@link GenericContainer} that implements the service being connected to.
	 * @return the {@link GenericContainer} providing the service
	 */
	public C getContainer() {
		return this.container;
	}

	@Override
	public Origin getOrigin() {
		return this.origin;
	}

	@Override
	public String toString() {
		return "ServiceConnectedContainer for %s".formatted(this.origin);
	}

}
