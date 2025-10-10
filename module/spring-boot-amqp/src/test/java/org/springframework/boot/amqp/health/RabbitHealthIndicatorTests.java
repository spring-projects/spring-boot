/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.amqp.health;

import java.util.Collections;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RabbitHealthIndicator}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class RabbitHealthIndicatorTests {

	@Mock
	@SuppressWarnings("NullAway.Init")
	private RabbitTemplate rabbitTemplate;

	@Mock
	@SuppressWarnings("NullAway.Init")
	private Channel channel;

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenRabbitTemplateIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new RabbitHealthIndicator(null))
			.withMessageContaining("'rabbitTemplate' must not be null");
	}

	@Test
	void healthWhenConnectionSucceedsShouldReturnUpWithVersion() {
		givenTemplateExecutionWillInvokeCallback();
		Connection connection = mock(Connection.class);
		given(this.channel.getConnection()).willReturn(connection);
		given(connection.getServerProperties()).willReturn(Collections.singletonMap("version", "123"));
		Health health = new RabbitHealthIndicator(this.rabbitTemplate).health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("version", "123");
	}

	@Test
	void healthWhenConnectionFailsShouldReturnDown() {
		givenTemplateExecutionWillInvokeCallback();
		given(this.channel.getConnection()).willThrow(new RuntimeException());
		Health health = new RabbitHealthIndicator(this.rabbitTemplate).health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

	private void givenTemplateExecutionWillInvokeCallback() {
		given(this.rabbitTemplate.execute(any())).willAnswer((invocation) -> {
			ChannelCallback<?> callback = invocation.getArgument(0);
			return callback.doInRabbit(this.channel);
		});
	}

}
