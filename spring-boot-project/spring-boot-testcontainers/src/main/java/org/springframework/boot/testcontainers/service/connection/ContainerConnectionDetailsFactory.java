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

import java.util.Arrays;

import org.testcontainers.containers.Container;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Base class for {@link ConnectionDetailsFactory} implementations that provide
 * {@link ConnectionDetails} from a {@link ContainerConnectionSource}.
 *
 * @param <D> the connection details type
 * @param <C> the container type
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public abstract class ContainerConnectionDetailsFactory<C extends Container<?>, D extends ConnectionDetails>
		implements ConnectionDetailsFactory<ContainerConnectionSource<C>, D> {

	/**
	 * Constant passed to the constructor when any connection name is accepted.
	 */
	protected static final String ANY_CONNECTION_NAME = null;

	private final String connectionName;

	private final String[] requiredClassNames;

	/**
	 * Create a new {@link ContainerConnectionDetailsFactory} instance that accepts
	 * {@link #ANY_CONNECTION_NAME any connection name}.
	 */
	protected ContainerConnectionDetailsFactory() {
		this(ANY_CONNECTION_NAME);
	}

	/**
	 * Create a new {@link ContainerConnectionDetailsFactory} instance with the given
	 * connection name restriction.
	 * @param connectionName the required connection name or {@link #ANY_CONNECTION_NAME}
	 * @param requiredClassNames the names of classes that must be present
	 */
	protected ContainerConnectionDetailsFactory(String connectionName, String... requiredClassNames) {
		this.connectionName = connectionName;
		this.requiredClassNames = requiredClassNames;
	}

	@Override
	public final D getConnectionDetails(ContainerConnectionSource<C> source) {
		if (!hasRequiredClasses()) {
			return null;
		}
		try {
			Class<?>[] generics = resolveGenerics();
			Class<?> containerType = generics[0];
			Class<?> connectionDetailsType = generics[1];
			if (source.accepts(this.connectionName, containerType, connectionDetailsType)) {
				return getContainerConnectionDetails(source);
			}
		}
		catch (NoClassDefFoundError ex) {
		}
		return null;
	}

	private boolean hasRequiredClasses() {
		return ObjectUtils.isEmpty(this.requiredClassNames) || Arrays.stream(this.requiredClassNames)
			.allMatch((requiredClassName) -> ClassUtils.isPresent(requiredClassName, null));
	}

	private Class<?>[] resolveGenerics() {
		return ResolvableType.forClass(ContainerConnectionDetailsFactory.class, getClass()).resolveGenerics();
	}

	/**
	 * Get the {@link ConnectionDetails} from the given {@link ContainerConnectionSource}
	 * {@code source}. May return {@code null} if no connection can be created. Result
	 * types should consider extending {@link ContainerConnectionDetails}.
	 * @param source the source
	 * @return the service connection or {@code null}.
	 */
	protected abstract D getContainerConnectionDetails(ContainerConnectionSource<C> source);

	/**
	 * Base class for {@link ConnectionDetails} results that are backed by a
	 * {@link ContainerConnectionSource}.
	 *
	 * @param <C> the container type
	 */
	protected static class ContainerConnectionDetails<C extends Container<?>>
			implements ConnectionDetails, OriginProvider, InitializingBean {

		private final ContainerConnectionSource<C> source;

		private volatile C container;

		/**
		 * Create a new {@link ContainerConnectionDetails} instance.
		 * @param source the source {@link ContainerConnectionSource}
		 */
		protected ContainerConnectionDetails(ContainerConnectionSource<C> source) {
			Assert.notNull(source, "Source must not be null");
			this.source = source;
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			this.container = this.source.getContainerSupplier().get();
		}

		/**
		 * Return the container that back this connection details instance. This method
		 * can only be called once the connection details bean has been initialized.
		 * @return the container instance
		 */
		protected final C getContainer() {
			Assert.state(this.container != null,
					"Container cannot be obtained before the connection details bean has been initialized");
			return this.container;
		}

		@Override
		public Origin getOrigin() {
			return this.source.getOrigin();
		}

	}

}
