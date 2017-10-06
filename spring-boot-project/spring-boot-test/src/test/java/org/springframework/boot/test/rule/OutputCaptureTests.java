/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.rule;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OutputCapture}.
 *
 * @author Roland Weisleder
 */
public class OutputCaptureTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void toStringShouldReturnAllCapturedOutput() throws Exception {
		System.out.println("Hello World");
		assertThat(this.outputCapture.toString()).contains("Hello World");
	}

	@Test
	public void reset() throws Exception {
		System.out.println("Hello");
		this.outputCapture.reset();
		System.out.println("World");
		assertThat(this.outputCapture.toString()).doesNotContain("Hello")
				.contains("World");
	}

}
