/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.jms;

import jakarta.jms.ConnectionFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConnectionFactoryUnwrapper}.
 *
 * @author Stephane Nicoll
 */
class ConnectionFactoryUnwrapperTests {

	@Nested
	class UnwrapCaching {

		@Test
		void unwrapWithSingleConnectionFactory() {
			ConnectionFactory connectionFactory = new SingleConnectionFactory();
			assertThat(unwrapCaching(connectionFactory)).isSameAs(connectionFactory);
		}

		@Test
		void unwrapWithConnectionFactory() {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			assertThat(unwrapCaching(connectionFactory)).isSameAs(connectionFactory);
		}

		@Test
		void unwrapWithCachingConnectionFactory() {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			assertThat(unwrapCaching(new CachingConnectionFactory(connectionFactory))).isSameAs(connectionFactory);
		}

		@Test
		void unwrapWithNestedCachingConnectionFactories() {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			CachingConnectionFactory firstCachingConnectionFactory = new CachingConnectionFactory(connectionFactory);
			CachingConnectionFactory secondCachingConnectionFactory = new CachingConnectionFactory(
					firstCachingConnectionFactory);
			assertThat(unwrapCaching(secondCachingConnectionFactory)).isSameAs(connectionFactory);
		}

		@Test
		void unwrapWithJmsPoolConnectionFactory() {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			JmsPoolConnectionFactory poolConnectionFactory = new JmsPoolConnectionFactory();
			poolConnectionFactory.setConnectionFactory(connectionFactory);
			assertThat(unwrapCaching(poolConnectionFactory)).isSameAs(poolConnectionFactory);
		}

		private ConnectionFactory unwrapCaching(ConnectionFactory connectionFactory) {
			return ConnectionFactoryUnwrapper.unwrapCaching(connectionFactory);
		}

	}

	@Nested
	class Unwrap {

		@Test
		void unwrapWithSingleConnectionFactory() {
			ConnectionFactory connectionFactory = new SingleConnectionFactory();
			assertThat(unwrap(connectionFactory)).isSameAs(connectionFactory);
		}

		@Test
		void unwrapWithConnectionFactory() {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			assertThat(unwrap(connectionFactory)).isSameAs(connectionFactory);
		}

		@Test
		void unwrapWithCachingConnectionFactory() {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			assertThat(unwrap(new CachingConnectionFactory(connectionFactory))).isSameAs(connectionFactory);
		}

		@Test
		void unwrapWithNestedCachingConnectionFactories() {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			CachingConnectionFactory firstCachingConnectionFactory = new CachingConnectionFactory(connectionFactory);
			CachingConnectionFactory secondCachingConnectionFactory = new CachingConnectionFactory(
					firstCachingConnectionFactory);
			assertThat(unwrap(secondCachingConnectionFactory)).isSameAs(connectionFactory);
		}

		@Test
		void unwrapWithJmsPoolConnectionFactory() {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			JmsPoolConnectionFactory poolConnectionFactory = new JmsPoolConnectionFactory();
			poolConnectionFactory.setConnectionFactory(connectionFactory);
			assertThat(unwrap(poolConnectionFactory)).isSameAs(connectionFactory);
		}

		@Test
		void unwrapWithNestedJmsPoolConnectionFactories() {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			JmsPoolConnectionFactory firstPooledConnectionFactory = new JmsPoolConnectionFactory();
			firstPooledConnectionFactory.setConnectionFactory(connectionFactory);
			JmsPoolConnectionFactory secondPooledConnectionFactory = new JmsPoolConnectionFactory();
			secondPooledConnectionFactory.setConnectionFactory(firstPooledConnectionFactory);
			assertThat(unwrap(secondPooledConnectionFactory)).isSameAs(connectionFactory);
		}

		@Test
		@ClassPathExclusions("pooled-jms-*")
		void unwrapWithoutJmsPoolOnClasspath() {
			assertThat(ClassUtils.isPresent("org.messaginghub.pooled.jms.JmsPoolConnectionFactory", null)).isFalse();
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			assertThat(unwrap(new CachingConnectionFactory(connectionFactory))).isSameAs(connectionFactory);
		}

		private ConnectionFactory unwrap(ConnectionFactory connectionFactory) {
			return ConnectionFactoryUnwrapper.unwrap(connectionFactory);
		}

	}

}
