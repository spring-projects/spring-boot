/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.health;

import com.rabbitmq.client.amqp.Connection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AmqpRabbitHealthIndicator}.
 *
 * @author Stephane Nicoll
 */
@ExtendWith(MockitoExtension.class)
class AmqpRabbitHealthIndicatorTests {

	@Mock
	private AmqpConnectionFactory amqpConnectionFactory;

	@Mock
	private Connection connection;

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenAmqpConnectionFactoryIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new AmqpRabbitHealthIndicator(null))
			.withMessageContaining("'amqpConnectionFactory' must not be null");
	}

	@Test
	void healthWhenConnectionSucceedsShouldReturnUpWithVersion() {
		given(this.amqpConnectionFactory.getConnection()).willReturn(this.connection);
		Connection.ConnectionInfo connectionInfo = mock(Connection.ConnectionInfo.class);
		given(connectionInfo.brokerVersion()).willReturn("123");
		given(this.connection.connectionInfo()).willReturn(connectionInfo);
		Health health = new AmqpRabbitHealthIndicator(this.amqpConnectionFactory).health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("version", "123");
	}

	@Test
	void healthWhenVersionIsMissingShouldReturnUpWithoutVersion() {
		given(this.amqpConnectionFactory.getConnection()).willReturn(this.connection);
		Connection.ConnectionInfo connectionInfo = mock(Connection.ConnectionInfo.class);
		given(connectionInfo.brokerVersion()).willReturn(null);
		given(this.connection.connectionInfo()).willReturn(connectionInfo);
		Health health = new AmqpRabbitHealthIndicator(this.amqpConnectionFactory).health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).doesNotContainKey("version");
	}

	@Test
	void healthWhenConnectionFailsShouldReturnDown() {
		given(this.amqpConnectionFactory.getConnection()).willThrow(new RuntimeException());
		Health health = new AmqpRabbitHealthIndicator(this.amqpConnectionFactory).health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

}
