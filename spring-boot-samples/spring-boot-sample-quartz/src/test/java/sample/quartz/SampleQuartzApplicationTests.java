/*
 * Copyright 2012-2018 the original author or authors.
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

package sample.quartz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.extension.OutputCapture;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleQuartzApplication}.
 *
 * @author Eddú Meléndez
 */
class SampleQuartzApplicationTests {

	@RegisterExtension
	OutputCapture output = new OutputCapture();

	@Test
	void quartzJobIsTriggered() throws InterruptedException {
		try (ConfigurableApplicationContext context = SpringApplication
				.run(SampleQuartzApplication.class)) {
			long end = System.currentTimeMillis() + 5000;
			while ((!this.output.toString().contains("Hello World!"))
					&& System.currentTimeMillis() < end) {
				Thread.sleep(100);
			}
			assertThat(this.output).contains("Hello World!");
		}
	}

}
