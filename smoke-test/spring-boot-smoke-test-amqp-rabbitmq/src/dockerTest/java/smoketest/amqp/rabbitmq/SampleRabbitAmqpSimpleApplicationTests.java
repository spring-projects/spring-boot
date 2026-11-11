/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.amqp.rabbitmq;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(OutputCaptureExtension.class)
class SampleRabbitAmqpSimpleApplicationTests {

	@Container
	@ServiceConnection
	static final RabbitMQContainer rabbit = TestImage.forContainer(RabbitMQContainer.class);

	@Autowired
	private Sender sender;

	@Test
	void sendSimpleMessage(CapturedOutput output) {
		this.sender.send("Test message");
		Awaitility.waitAtMost(Duration.ofMinutes(1))
			.untilAsserted(() -> assertThat(output).contains("'Test message'").contains("'smoke-test'"));
	}

}
