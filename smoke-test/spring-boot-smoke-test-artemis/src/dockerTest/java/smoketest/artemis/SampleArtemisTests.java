/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.artemis;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for demo application.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(OutputCaptureExtension.class)
class SampleArtemisTests {

	@Container
	@ServiceConnection
	static final ArtemisContainer container = TestImage.ARTEMIS_LEGACY.container();

	@Autowired
	private Producer producer;

	@Test
	void sendSimpleMessage(CapturedOutput output) {
		this.producer.send("Test message");
		Awaitility.waitAtMost(Duration.ofMinutes(1)).untilAsserted(() -> assertThat(output).contains("Test message"));
	}

}
