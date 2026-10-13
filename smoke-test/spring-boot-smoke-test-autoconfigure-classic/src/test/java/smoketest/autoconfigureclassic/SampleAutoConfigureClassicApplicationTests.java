/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.autoconfigureclassic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleAutoConfigureClassicApplication}.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class SampleAutoConfigureClassicApplicationTests {

	@Test
	void testApplicationRuns(CapturedOutput output) {
		SampleAutoConfigureClassicApplication.main(new String[0]);
		assertThat(output).contains("Auto-Configure Classic!");
	}

}
