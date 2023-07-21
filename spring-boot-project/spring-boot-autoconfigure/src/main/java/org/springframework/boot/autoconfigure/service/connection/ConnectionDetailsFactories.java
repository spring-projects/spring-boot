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

package org.springframework.boot.autoconfigure.service.connection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.FailureHandler;
import org.springframework.util.Assert;

/**
 * A registry of {@link ConnectionDetailsFactory} instances.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public class ConnectionDetailsFactories {

	private static final Log logger = LogFactory.getLog(ConnectionDetailsFactories.class);

	private final List<Registration<?, ?>> registrations = new ArrayList<>();

	public ConnectionDetailsFactories() {
		this(SpringFactoriesLoader.forDefaultResourceLocation(ConnectionDetailsFactory.class.getClassLoader()));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	ConnectionDetailsFactories(SpringFactoriesLoader loader) {
		List<ConnectionDetailsFactory> factories = loader.load(ConnectionDetailsFactory.class,
				FailureHandler.logging(logger));
		Stream<Registration<?, ?>> registrations = factories.stream().map(Registration::get);
		registrations.filter(Objects::nonNull).forEach(this.registrations::add);
	}

	/**
	 * Return a {@link Map} of {@link ConnectionDetails} interface type to
	 * {@link ConnectionDetails} instance created from the factories associated with the
	 * given source.
	 * @param <S> the source type
	 * @param source the source
	 * @param required if a connection details result is required
	 * @return a map of {@link ConnectionDetails} instances
	 * @throws ConnectionDetailsFactoryNotFoundException if a result is required but no
	 * connection details factory is registered for the source
	 * @throws ConnectionDetailsNotFoundException if a result is required but no
	 * connection details instance was created from a registered factory
	 */
	public <S> Map<Class<?>, ConnectionDetails> getConnectionDetails(S source, boolean required)
			throws ConnectionDetailsFactoryNotFoundException, ConnectionDetailsNotFoundException {
		List<Registration<S, ?>> registrations = getRegistrations(source, required);
		Map<Class<?>, ConnectionDetails> result = new LinkedHashMap<>();
		for (Registration<S, ?> registration : registrations) {
			ConnectionDetails connectionDetails = registration.factory().getConnectionDetails(source);
			if (connectionDetails != null) {
				Class<?> connectionDetailsType = registration.connectionDetailsType();
				ConnectionDetails previous = result.put(connectionDetailsType, connectionDetails);
				Assert.state(previous == null, () -> "Duplicate connection details supplied for %s"
					.formatted(connectionDetailsType.getName()));
			}
		}
		if (required && result.isEmpty()) {
			throw new ConnectionDetailsNotFoundException(source);
		}
		return Map.copyOf(result);
	}

	@SuppressWarnings("unchecked")
	<S> List<Registration<S, ?>> getRegistrations(S source, boolean required) {
		Class<S> sourceType = (Class<S>) source.getClass();
		List<Registration<S, ?>> result = new ArrayList<>();
		for (Registration<?, ?> candidate : this.registrations) {
			if (candidate.sourceType().isAssignableFrom(sourceType)) {
				result.add((Registration<S, ?>) candidate);
			}
		}
		if (required && result.isEmpty()) {
			throw new ConnectionDetailsFactoryNotFoundException(source);
		}
		result.sort(Comparator.comparing(Registration::factory, AnnotationAwareOrderComparator.INSTANCE));
		return List.copyOf(result);
	}

	/**
	 * A {@link ConnectionDetailsFactory} registration.
	 *
	 * @param <S> the source type
	 * @param <D> the connection details type
	 * @param sourceType the source type
	 * @param connectionDetailsType the connection details type
	 * @param factory the factory
	 */
	record Registration<S, D extends ConnectionDetails>(Class<S> sourceType, Class<D> connectionDetailsType,
			ConnectionDetailsFactory<S, D> factory) {

		@SuppressWarnings("unchecked")
		private static <S, D extends ConnectionDetails> Registration<S, D> get(ConnectionDetailsFactory<S, D> factory) {
			ResolvableType type = ResolvableType.forClass(ConnectionDetailsFactory.class, factory.getClass());
			if (!type.hasUnresolvableGenerics()) {
				Class<?>[] generics = type.resolveGenerics();
				return new Registration<>((Class<S>) generics[0], (Class<D>) generics[1], factory);
			}
			return null;
		}

	}

}
