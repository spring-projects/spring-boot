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

package smoketest.amqp;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(OutputCaptureExtension.class)
class SampleRabbitAmqpSimpleApplicationTests {

	@Container
	@ServiceConnection
	static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

	@Autowired
	private Sender sender;

	@Test
	void sendSimpleMessage(CapturedOutput output) {
		this.sender.send("Test message");
		Awaitility.waitAtMost(Duration.ofMinutes(1)).untilAsserted(() -> assertThat(output).contains("Test message"));
	}

}
