/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.velocity;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for Velocity application with no web layer.
 *
 * @author Dave Syer
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SampleVelocityApplicationTests {

	@ClassRule
	public static OutputCapture output = new OutputCapture();

	@Test
	public void testVelocityTemplate() throws Exception {
		String result = SampleVelocityApplicationTests.output.toString();
		assertThat(result).contains("Hello, Andy");
	}

}
