/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.activemq.embedded;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ActiveMQ smoke test with embedded broker.
 *
 * @author Stephane Nicoll
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class SampleActiveMQApplicationTests {

	@Autowired
	private Producer producer;

	@Test
	void sendSimpleMessage(CapturedOutput output) throws InterruptedException {
		this.producer.send("Test message");
		Thread.sleep(1000L);
		assertThat(output).contains("Test message");
	}

}
