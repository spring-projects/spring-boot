/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.system;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OutputCaptureRule}.
 *
 * @author Roland Weisleder
 */
public class OutputCaptureRuleTests {

	@Rule
	public OutputCaptureRule output = new OutputCaptureRule();

	@Test
	public void toStringShouldReturnAllCapturedOutput() {
		System.out.println("Hello World");
		assertThat(this.output.toString()).contains("Hello World");
	}

	@Test
	public void getAllShouldReturnAllCapturedOutput() {
		System.out.println("Hello World");
		System.err.println("Hello Error");
		assertThat(this.output.getAll()).contains("Hello World", "Hello Error");
	}

	@Test
	public void getOutShouldOnlyReturnOutputCapturedFromSystemOut() {
		System.out.println("Hello World");
		System.err.println("Hello Error");
		assertThat(this.output.getOut()).contains("Hello World");
		assertThat(this.output.getOut()).doesNotContain("Hello Error");
	}

	@Test
	public void getErrShouldOnlyReturnOutputCapturedFromSystemErr() {
		System.out.println("Hello World");
		System.err.println("Hello Error");
		assertThat(this.output.getErr()).contains("Hello Error");
		assertThat(this.output.getErr()).doesNotContain("Hello World");
	}

	@Test
	public void captureShouldBeAssertable() {
		System.out.println("Hello World");
		assertThat(this.output).contains("Hello World");
	}

}
