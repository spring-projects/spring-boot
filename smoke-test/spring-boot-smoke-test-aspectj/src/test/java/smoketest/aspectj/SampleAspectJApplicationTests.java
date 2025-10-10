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
