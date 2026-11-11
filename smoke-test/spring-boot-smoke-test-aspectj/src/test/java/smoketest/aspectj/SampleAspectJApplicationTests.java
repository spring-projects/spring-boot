/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.aspectj;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleAspectJApplication}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class SampleAspectJApplicationTests {

	private String profiles;

	@BeforeEach
	void init() {
		this.profiles = System.getProperty("spring.profiles.active");
	}

	@AfterEach
	void after() {
		if (this.profiles != null) {
			System.setProperty("spring.profiles.active", this.profiles);
		}
		else {
			System.clearProperty("spring.profiles.active");
		}
	}

	@Test
	void testDefaultSettings(CapturedOutput output) {
		SampleAspectJApplication.main(new String[0]);
		assertThat(output).contains("Hello Phil");
	}

	@Test
	void testCommandLineOverrides(CapturedOutput output) {
		SampleAspectJApplication.main(new String[] { "--test.name=Gordon" });
		assertThat(output).contains("Hello Gordon");
	}

}
