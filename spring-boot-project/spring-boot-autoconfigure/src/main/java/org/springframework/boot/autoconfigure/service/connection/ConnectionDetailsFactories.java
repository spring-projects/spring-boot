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
import java.util.stream.Stream;

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

	private List<Registration<?, ?>> registrations = new ArrayList<>();

	public ConnectionDetailsFactories() {
		this(SpringFactoriesLoader.forDefaultResourceLocation(ConnectionDetailsFactory.class.getClassLoader()));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	ConnectionDetailsFactories(SpringFactoriesLoader loader) {
		List<ConnectionDetailsFactory> factories = loader.load(ConnectionDetailsFactory.class);
		Stream<Registration<?, ?>> registrations = factories.stream().map(Registration::get);
		registrations.filter(Objects::nonNull).forEach(this.registrations::add);
	}

	public <S> ConnectionDetails getConnectionDetails(S source) {
		return getConnectionDetailsFactory(source).getConnectionDetails(source);
	}

	@SuppressWarnings("unchecked")
	public <S> ConnectionDetailsFactory<S, ConnectionDetails> getConnectionDetailsFactory(S source) {
		Class<S> sourceType = (Class<S>) source.getClass();
		List<ConnectionDetailsFactory<S, ConnectionDetails>> result = new ArrayList<>();
		for (Registration<?, ?> candidate : this.registrations) {
			if (candidate.sourceType().isAssignableFrom(sourceType)) {
				result.add((ConnectionDetailsFactory<S, ConnectionDetails>) candidate.factory());
			}
		}
		if (result.isEmpty()) {
			throw new ConnectionDetailsFactoryNotFoundException(source);
		}
		AnnotationAwareOrderComparator.sort(result);
		return (result.size() != 1) ? new CompositeConnectionDetailsFactory<>(result) : result.get(0);
	}

	/**
	 * A {@link ConnectionDetailsFactory} registration.
	 */
	private record Registration<S, D extends ConnectionDetails>(Class<S> sourceType, Class<D> connectionDetailsType,
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

	/**
	 * Composite {@link ConnectionDetailsFactory} implementation.
	 *
	 * @param <S> the source type
	 */
	static class CompositeConnectionDetailsFactory<S> implements ConnectionDetailsFactory<S, ConnectionDetails> {

		private final List<ConnectionDetailsFactory<S, ConnectionDetails>> delegates;

		CompositeConnectionDetailsFactory(List<ConnectionDetailsFactory<S, ConnectionDetails>> delegates) {
			this.delegates = delegates;
		}

		@Override
		public ConnectionDetails getConnectionDetails(S source) {
			return this.delegates.stream()
				.map((delegate) -> delegate.getConnectionDetails(source))
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);
		}

		List<ConnectionDetailsFactory<S, ConnectionDetails>> getDelegates() {
			return this.delegates;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("delegates", this.delegates).toString();
		}

	}

}
