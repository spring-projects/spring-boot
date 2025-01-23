/*
 * Copyright 2012-2025 the original author or authors.
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
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.lifecycle.Startable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersStartup;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.FailureHandler;
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

	private final List<String> connectionNames;

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
		this(Arrays.asList(connectionName), requiredClassNames);
	}

	/**
	 * Create a new {@link ContainerConnectionDetailsFactory} instance with the given
	 * supported connection names.
	 * @param connectionNames the supported connection names
	 * @param requiredClassNames the names of classes that must be present
	 * @since 3.4.0
	 */
	protected ContainerConnectionDetailsFactory(List<String> connectionNames, String... requiredClassNames) {
		Assert.notEmpty(connectionNames, "'connectionNames' must not be empty");
		this.connectionNames = connectionNames;
		this.requiredClassNames = requiredClassNames;
	}

	@Override
	public final D getConnectionDetails(ContainerConnectionSource<C> source) {
		if (!hasRequiredClasses()) {
			return null;
		}
		try {
			Class<?>[] generics = resolveGenerics();
			Class<?> requiredContainerType = generics[0];
			Class<?> requiredConnectionDetailsType = generics[1];
			if (sourceAccepts(source, requiredContainerType, requiredConnectionDetailsType)) {
				return getContainerConnectionDetails(source);
			}
		}
		catch (NoClassDefFoundError ex) {
			// Ignore
		}
		return null;
	}

	/**
	 * Return if the given source accepts the connection. By default this method checks
	 * each connection name.
	 * @param source the container connection source
	 * @param requiredContainerType the required container type
	 * @param requiredConnectionDetailsType the required connection details type
	 * @return if the source accepts the connection
	 * @since 3.4.0
	 */
	protected boolean sourceAccepts(ContainerConnectionSource<C> source, Class<?> requiredContainerType,
			Class<?> requiredConnectionDetailsType) {
		for (String requiredConnectionName : this.connectionNames) {
			if (source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType)) {
				return true;
			}
		}
		return false;
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
			implements ConnectionDetails, OriginProvider, InitializingBean, ApplicationContextAware {

		private final ContainerConnectionSource<C> source;

		private volatile C container;

		/**
		 * Create a new {@link ContainerConnectionDetails} instance.
		 * @param source the source {@link ContainerConnectionSource}
		 */
		protected ContainerConnectionDetails(ContainerConnectionSource<C> source) {
			Assert.notNull(source, "'source' must not be null");
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
			if (this.container instanceof Startable startable) {
				TestcontainersStartup.start(startable);
			}
			return this.container;
		}

		@Override
		public Origin getOrigin() {
			return this.source.getOrigin();
		}

		@Override
		@Deprecated(since = "3.4.0", forRemoval = true)
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		}

	}

	static class ContainerConnectionDetailsFactoriesRuntimeHints implements RuntimeHintsRegistrar {

		private static final Log logger = LogFactory.getLog(ContainerConnectionDetailsFactoriesRuntimeHints.class);

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			SpringFactoriesLoader.forDefaultResourceLocation(classLoader)
				.load(ConnectionDetailsFactory.class, FailureHandler.logging(logger))
				.stream()
				.flatMap(this::requiredClassNames)
				.forEach((requiredClassName) -> hints.reflection()
					.registerTypeIfPresent(classLoader, requiredClassName));
		}

		private Stream<String> requiredClassNames(ConnectionDetailsFactory<?, ?> connectionDetailsFactory) {
			return (connectionDetailsFactory instanceof ContainerConnectionDetailsFactory<?, ?> containerConnectionDetailsFactory)
					? Stream.of(containerConnectionDetailsFactory.requiredClassNames) : Stream.empty();
		}

	}

}
