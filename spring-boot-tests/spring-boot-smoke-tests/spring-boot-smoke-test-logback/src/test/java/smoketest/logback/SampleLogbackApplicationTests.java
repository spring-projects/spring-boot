/*
 * Copyright 2012-2021 the original author or authors.
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

}
