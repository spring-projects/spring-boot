/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class SampleBatchApplicationTests {

	@Test
	void testDefaultSettings(CapturedOutput output) {
		assertThat(SpringApplication.exit(SpringApplication.run(SampleBatchApplication.class))).isZero();
		assertThat(output).contains("completed with the following parameters");
	}

}
