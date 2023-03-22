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
import java.util.List;
import java.util.Objects;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.style.ToStringCreator;

/**
 * A registry of {@link ConnectionDetailsFactory} instances.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public class ConnectionDetailsFactories {

	private List<FactoryDetails> registeredFactories = new ArrayList<>();

	public ConnectionDetailsFactories() {
		this(SpringFactoriesLoader.forDefaultResourceLocation(ConnectionDetailsFactory.class.getClassLoader()));
	}

	@SuppressWarnings("rawtypes")
	ConnectionDetailsFactories(SpringFactoriesLoader loader) {
		List<ConnectionDetailsFactory> factories = loader.load(ConnectionDetailsFactory.class);
		factories.stream().map(this::factoryDetails).filter(Objects::nonNull).forEach(this::register);
	}

	@SuppressWarnings("unchecked")
	private FactoryDetails factoryDetails(ConnectionDetailsFactory<?, ?> factory) {
		ResolvableType connectionDetailsFactory = findConnectionDetailsFactory(
				ResolvableType.forClass(factory.getClass()));
		if (connectionDetailsFactory != null) {
			ResolvableType input = connectionDetailsFactory.getGeneric(0);
			ResolvableType output = connectionDetailsFactory.getGeneric(1);
			return new FactoryDetails(input.getRawClass(), (Class<? extends ConnectionDetails>) output.getRawClass(),
					factory);
		}
		return null;
	}

	private ResolvableType findConnectionDetailsFactory(ResolvableType type) {
		try {
			ResolvableType[] interfaces = type.getInterfaces();
			for (ResolvableType iface : interfaces) {
				if (iface.getRawClass().equals(ConnectionDetailsFactory.class)) {
					return iface;
				}
			}
		}
		catch (TypeNotPresentException ex) {
			// A type referenced by the factory is not present. Skip it.
		}
		ResolvableType superType = type.getSuperType();
		return ResolvableType.NONE.equals(superType) ? null : findConnectionDetailsFactory(superType);
	}

	private void register(FactoryDetails details) {
		this.registeredFactories.add(details);
	}

	@SuppressWarnings("unchecked")
	public <S> ConnectionDetailsFactory<S, ConnectionDetails> getConnectionDetailsFactory(S source) {
		Class<S> input = (Class<S>) source.getClass();
		List<ConnectionDetailsFactory<S, ConnectionDetails>> matchingFactories = new ArrayList<>();
		for (FactoryDetails factoryDetails : this.registeredFactories) {
			if (factoryDetails.input.isAssignableFrom(input)) {
				matchingFactories.add((ConnectionDetailsFactory<S, ConnectionDetails>) factoryDetails.factory);
			}
		}
		if (matchingFactories.isEmpty()) {
			throw new ConnectionDetailsFactoryNotFoundException(source);
		}
		else {
			if (matchingFactories.size() == 1) {
				return matchingFactories.get(0);
			}
			AnnotationAwareOrderComparator.sort(matchingFactories);
			return new CompositeConnectionDetailsFactory<>(matchingFactories);
		}
	}

	private record FactoryDetails(Class<?> input, Class<? extends ConnectionDetails> output,
			ConnectionDetailsFactory<?, ?> factory) {
	}

	static class CompositeConnectionDetailsFactory<S> implements ConnectionDetailsFactory<S, ConnectionDetails> {

		private final List<ConnectionDetailsFactory<S, ConnectionDetails>> delegates;

		CompositeConnectionDetailsFactory(List<ConnectionDetailsFactory<S, ConnectionDetails>> delegates) {
			this.delegates = delegates;
		}

		@Override
		@SuppressWarnings("unchecked")
		public ConnectionDetails getConnectionDetails(Object source) {
			for (ConnectionDetailsFactory<S, ConnectionDetails> delegate : this.delegates) {
				ConnectionDetails connectionDetails = delegate.getConnectionDetails((S) source);
				if (connectionDetails != null) {
					return connectionDetails;
				}
			}
			return null;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("delegates", this.delegates).toString();
		}

		List<ConnectionDetailsFactory<S, ConnectionDetails>> getDelegates() {
			return this.delegates;
		}

	}

}
