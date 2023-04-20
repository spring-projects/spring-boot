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

package smoketest.quartz;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

import static org.hamcrest.Matchers.containsString;

/**
 * Tests for {@link SampleQuartzApplication}.
 *
 * @author Eddú Meléndez
 */
@ExtendWith(OutputCaptureExtension.class)
class SampleQuartzApplicationTests {

	@Test
	void quartzJobIsTriggered(CapturedOutput output) {
		try (ConfigurableApplicationContext context = SpringApplication.run(SampleQuartzApplication.class,
				"--server.port=0")) {
			Awaitility.waitAtMost(Duration.ofSeconds(5)).until(output::toString, containsString("Hello World!"));
		}
	}

}
