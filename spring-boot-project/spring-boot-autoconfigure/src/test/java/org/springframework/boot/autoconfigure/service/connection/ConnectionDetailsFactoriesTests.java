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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories.Registration;
import org.springframework.core.Ordered;
import org.springframework.core.test.io.support.MockSpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link ConnectionDetailsFactories}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ConnectionDetailsFactoriesTests {

	private final MockSpringFactoriesLoader loader = new MockSpringFactoriesLoader();

	@Test
	void getRequiredConnectionDetailsWhenNoFactoryForSourceThrowsException() {
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories(this.loader);
		assertThatExceptionOfType(ConnectionDetailsFactoryNotFoundException.class)
			.isThrownBy(() -> factories.getConnectionDetails("source", true));
	}

	@Test
	void getOptionalConnectionDetailsWhenNoFactoryForSourceThrowsException() {
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories(this.loader);
		assertThat(factories.getConnectionDetails("source", false)).isEmpty();
	}

	@Test
	void getConnectionDetailsWhenSourceHasOneMatchReturnsSingleResult() {
		this.loader.addInstance(ConnectionDetailsFactory.class, new TestConnectionDetailsFactory());
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories(this.loader);
		Map<Class<?>, ConnectionDetails> connectionDetails = factories.getConnectionDetails("source", false);
		assertThat(connectionDetails).hasSize(1);
		assertThat(connectionDetails.get(TestConnectionDetails.class)).isInstanceOf(TestConnectionDetailsImpl.class);
	}

	@Test
	void getRequiredConnectionDetailsWhenSourceHasNoMatchTheowsException() {
		this.loader.addInstance(ConnectionDetailsFactory.class, new NullResultTestConnectionDetailsFactory());
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories(this.loader);
		assertThatExceptionOfType(ConnectionDetailsNotFoundException.class)
			.isThrownBy(() -> factories.getConnectionDetails("source", true));
	}

	@Test
	void getOptionalConnectionDetailsWhenSourceHasNoMatchReturnsEmptyMap() {
		this.loader.addInstance(ConnectionDetailsFactory.class, new NullResultTestConnectionDetailsFactory());
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories(this.loader);
		Map<Class<?>, ConnectionDetails> connectionDetails = factories.getConnectionDetails("source", false);
		assertThat(connectionDetails).isEmpty();
	}

	@Test
	void getConnectionDetailsWhenSourceHasMultipleMatchesReturnsMultipleResults() {
		this.loader.addInstance(ConnectionDetailsFactory.class, new TestConnectionDetailsFactory(),
				new OtherConnectionDetailsFactory());
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories(this.loader);
		Map<Class<?>, ConnectionDetails> connectionDetails = factories.getConnectionDetails("source", false);
		assertThat(connectionDetails).hasSize(2);
	}

	@Test
	void getConnectionDetailsWhenDuplicatesThrowsException() {
		this.loader.addInstance(ConnectionDetailsFactory.class, new TestConnectionDetailsFactory(),
				new TestConnectionDetailsFactory());
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories(this.loader);
		assertThatIllegalStateException().isThrownBy(() -> factories.getConnectionDetails("source", false))
			.withMessage("Duplicate connection details supplied for " + TestConnectionDetails.class.getName());
	}

	@Test
	void getRegistrationsReturnsOrderedDelegates() {
		TestConnectionDetailsFactory orderOne = new TestConnectionDetailsFactory(1);
		TestConnectionDetailsFactory orderTwo = new TestConnectionDetailsFactory(2);
		TestConnectionDetailsFactory orderThree = new TestConnectionDetailsFactory(3);
		this.loader.addInstance(ConnectionDetailsFactory.class, orderOne, orderThree, orderTwo);
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories(this.loader);
		List<Registration<String, ?>> registrations = factories.getRegistrations("source", false);
		assertThat(registrations.get(0).factory()).isEqualTo(orderOne);
		assertThat(registrations.get(1).factory()).isEqualTo(orderTwo);
		assertThat(registrations.get(2).factory()).isEqualTo(orderThree);
	}

	@Test
	void factoryLoadFailureDoesNotPreventOtherFactoriesFromLoading() {
		this.loader.add(ConnectionDetailsFactory.class.getName(), "com.example.NonExistentConnectionDetailsFactory");
		assertThatNoException().isThrownBy(() -> new ConnectionDetailsFactories(this.loader));
	}

	private static final class TestConnectionDetailsFactory
			implements ConnectionDetailsFactory<String, TestConnectionDetails>, Ordered {

		private final int order;

		private TestConnectionDetailsFactory() {
			this(0);
		}

		private TestConnectionDetailsFactory(int order) {
			this.order = order;
		}

		@Override
		public TestConnectionDetails getConnectionDetails(String source) {
			return new TestConnectionDetailsImpl();
		}

		@Override
		public int getOrder() {
			return this.order;
		}

	}

	private static final class NullResultTestConnectionDetailsFactory
			implements ConnectionDetailsFactory<String, TestConnectionDetails> {

		@Override
		public TestConnectionDetails getConnectionDetails(String source) {
			return null;
		}

	}

	private static final class OtherConnectionDetailsFactory
			implements ConnectionDetailsFactory<String, OtherConnectionDetails> {

		@Override
		public OtherConnectionDetails getConnectionDetails(String source) {
			return new OtherConnectionDetailsImpl();
		}

	}

	private interface TestConnectionDetails extends ConnectionDetails {

	}

	private static final class TestConnectionDetailsImpl implements TestConnectionDetails {

	}

	private interface OtherConnectionDetails extends ConnectionDetails {

	}

	private static final class OtherConnectionDetailsImpl implements OtherConnectionDetails {

	}

}
