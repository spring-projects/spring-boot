/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.amqp;

import org.junit.Assert;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

/**
 * Tests for {@link MultiRabbitConnectionFactoryWrapper}.
 *
 * @author Wander Costa
 */
@ExtendWith(MockitoExtension.class)
class MultiRabbitConnectionFactoryWrapperTest {

	private static final String DUMMY_KEY = "dummy-key";

	@Mock
	private ConnectionFactory connectionFactory;

	@Mock
	private SimpleRabbitListenerContainerFactory containerFactory;

	@Mock
	private RabbitAdmin rabbitAdmin;

	private final MultiRabbitConnectionFactoryWrapper wrapper = new MultiRabbitConnectionFactoryWrapper();

	@Test
	void shouldGetDefaultConnectionFactory() {
		this.wrapper.setDefaultConnectionFactory(this.connectionFactory);
		Assert.assertSame(this.connectionFactory, this.wrapper.getDefaultConnectionFactory());
	}

	@Test
	void shouldSetNullDefaultConnectionFactory() {
		this.wrapper.setDefaultConnectionFactory(null);
		Assert.assertNull(this.wrapper.getDefaultConnectionFactory());
	}

	@Test
	void shouldAddConnectionFactory() {
		this.wrapper.addConnectionFactory(DUMMY_KEY, this.connectionFactory);
		Assert.assertSame(this.connectionFactory, this.wrapper.getConnectionFactories().get(DUMMY_KEY));
	}

	@Test
	void shouldNotAddNullConnectionFactory() {
		final ThrowingRunnable runnable = () -> this.wrapper.addConnectionFactory(DUMMY_KEY, null);
		Assert.assertThrows("ConnectionFactory may not be null", IllegalArgumentException.class, runnable);
	}

	@Test
	void shouldNotAddConnectionFactoryWithEmptyKey() {
		final ThrowingRunnable runnable = () -> this.wrapper.addConnectionFactory("", this.connectionFactory);
		Assert.assertThrows("Key may not be null or empty", IllegalArgumentException.class, runnable);
	}

}
