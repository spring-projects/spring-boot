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

package org.springframework.boot.test.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OutputCaptureExtension} when test methods are executed concurrently.
 *
 * @author Sam Brannen
 */
@ExtendWith(OutputCaptureExtension.class)
@Execution(ExecutionMode.CONCURRENT)
@Disabled("OutputCaptureExtension currently does not support concurrent execution")
class ConcurrentOutputCaptureExtensionTests {

	@BeforeEach
	void beforeEach() {
		System.out.println("beforeEach");
	}

	@RepeatedTest(5)
	void captureOutput(RepetitionInfo repetitionInfo, CapturedOutput output) {
		String testOutput = "test output: " + repetitionInfo.getCurrentRepetition();
		String testError = "test error: " + repetitionInfo.getCurrentRepetition();

		System.out.println(testOutput);
		System.err.println(testError);

		assertThat(lines(output))//
				.containsExactlyInAnyOrder("beforeEach", testOutput, testError);
		assertThat(lines(output.getOut()))//
				.containsExactly("beforeEach", testOutput);
		assertThat(lines(output.getErr()))//
				.containsExactly(testError);
	}

	private String[] lines(CharSequence text) {
		return text.toString().split(System.lineSeparator());
	}

	@AfterEach
	void after(CapturedOutput output) {
		assertThat(output.getOut()).contains("beforeEach", "test output");
		assertThat(output.getErr()).contains("test error");
	}

}
