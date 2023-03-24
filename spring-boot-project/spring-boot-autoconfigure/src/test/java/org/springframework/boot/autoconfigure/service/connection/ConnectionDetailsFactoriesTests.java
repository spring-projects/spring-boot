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

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories.CompositeConnectionDetailsFactory;
import org.springframework.core.Ordered;
import org.springframework.core.test.io.support.MockSpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
	void getConnectionDetailsFactoryShouldThrowWhenNoFactoryForSource() {
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories(this.loader);
		assertThatExceptionOfType(ConnectionDetailsFactoryNotFoundException.class)
			.isThrownBy(() -> factories.getConnectionDetailsFactory("source"));
	}

	@Test
	void getConnectionDetailsFactoryShouldReturnSingleFactoryWhenSourceHasOneMatch() {
		this.loader.addInstance(ConnectionDetailsFactory.class, new TestConnectionDetailsFactory());
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories(this.loader);
		ConnectionDetailsFactory<String, ConnectionDetails> factory = factories.getConnectionDetailsFactory("source");
		assertThat(factory).isInstanceOf(TestConnectionDetailsFactory.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void getConnectionDetailsFactoryShouldReturnCompositeFactoryWhenSourceHasMultipleMatches() {
		this.loader.addInstance(ConnectionDetailsFactory.class, new TestConnectionDetailsFactory(),
				new TestConnectionDetailsFactory());
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories(this.loader);
		ConnectionDetailsFactory<String, ConnectionDetails> factory = factories.getConnectionDetailsFactory("source");
		assertThat(factory).asInstanceOf(InstanceOfAssertFactories.type(CompositeConnectionDetailsFactory.class))
			.satisfies((composite) -> assertThat(composite.getDelegates()).hasSize(2));
	}

	@Test
	@SuppressWarnings("unchecked")
	void compositeFactoryShouldHaveOrderedDelegates() {
		TestConnectionDetailsFactory orderOne = new TestConnectionDetailsFactory(1);
		TestConnectionDetailsFactory orderTwo = new TestConnectionDetailsFactory(2);
		TestConnectionDetailsFactory orderThree = new TestConnectionDetailsFactory(3);
		this.loader.addInstance(ConnectionDetailsFactory.class, orderOne, orderThree, orderTwo);
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories(this.loader);
		ConnectionDetailsFactory<String, ConnectionDetails> factory = factories.getConnectionDetailsFactory("source");
		assertThat(factory).asInstanceOf(InstanceOfAssertFactories.type(CompositeConnectionDetailsFactory.class))
			.satisfies((composite) -> assertThat(composite.getDelegates()).containsExactly(orderOne, orderTwo,
					orderThree));
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
			return new TestConnectionDetails();
		}

		@Override
		public int getOrder() {
			return this.order;
		}

	}

	private static final class TestConnectionDetails implements ConnectionDetails {

		private TestConnectionDetails() {
		}

	}

}
