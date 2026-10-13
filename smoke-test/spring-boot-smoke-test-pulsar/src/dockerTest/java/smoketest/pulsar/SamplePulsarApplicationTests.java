/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.pulsar;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.pulsar.PulsarContainer;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;

@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
class SamplePulsarApplicationTests {

	@Container
	@ServiceConnection
	static final PulsarContainer pulsar = TestImage.forContainer(PulsarContainer.class);

	@Test
	void appProducesAndConsumesMessages(CapturedOutput output) {
		List<String> expectedOutput = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			expectedOutput.add("++++++PRODUCE:(%s)------".formatted(i));
			expectedOutput.add("++++++CONSUME:(%s)------".formatted(i));
		}
		waitAtMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(output).contains(expectedOutput));
	}

}
