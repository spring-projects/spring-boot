/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.logback;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class SampleLogbackApplicationTests {

	@Test
	void testLoadedCustomLogbackConfig(CapturedOutput output) {
		SampleLogbackApplication.main(new String[0]);
		assertThat(output).contains("Sample Debug Message").doesNotContain("Sample Trace Message");
	}

	@Test
	void testProfile(CapturedOutput output) {
		SampleLogbackApplication.main(new String[] { "--spring.profiles.active=staging" });
		assertThat(output).contains("Sample Debug Message").contains("Sample Trace Message");
	}

	@Test
	void testDisableConsoleLogging(CapturedOutput output) {
		SampleLogbackApplication.main(new String[] { "--logging.console.enabled=false" });
		assertThat(output).doesNotContain("---");
	}

}
