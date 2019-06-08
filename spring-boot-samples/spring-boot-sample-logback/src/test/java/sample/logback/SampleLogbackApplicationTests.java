/*
 * Copyright 2012-2019 the original author or authors.
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

package sample.logback;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.test.rule.OutputCapture;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class SampleLogbackApplicationTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void testLoadedCustomLogbackConfig() throws Exception {
		SampleLogbackApplication.main(new String[0]);
		this.outputCapture.expect(containsString("Sample Debug Message"));
		this.outputCapture.expect(not(containsString("Sample Trace Message")));
	}

	@Test
	public void testProfile() throws Exception {
		SampleLogbackApplication.main(new String[] { "--spring.profiles.active=staging" });
		this.outputCapture.expect(containsString("Sample Debug Message"));
		this.outputCapture.expect(containsString("Sample Trace Message"));
	}

}
