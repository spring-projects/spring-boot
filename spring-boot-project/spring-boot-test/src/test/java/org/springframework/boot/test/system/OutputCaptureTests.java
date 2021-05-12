/*
 * Copyright 2012-2020 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link OutputCapture}.
 *
 * @author Phillip Webb
 */
class OutputCaptureTests {

	private PrintStream originalOut;

	private PrintStream originalErr;

	private TestPrintStream systemOut;

	private TestPrintStream systemErr;

	private OutputCapture output = new OutputCapture();

	@BeforeEach
	void replaceSystemStreams() {
		this.originalOut = System.out;
		this.originalErr = System.err;
		this.systemOut = new TestPrintStream();
		this.systemErr = new TestPrintStream();
		System.setOut(this.systemOut);
		System.setErr(this.systemErr);
	}

	@AfterEach
	void restoreSystemStreams() {
		System.setOut(this.originalOut);
		System.setErr(this.originalErr);
	}

	@Test
	void pushWhenEmptyStartsCapture() {
		System.out.print("A");
		this.output.push();
		System.out.print("B");
		assertThat(this.output).isEqualTo("B");
	}

	@Test
	void pushWhenHasExistingStartsNewCapture() {
		System.out.print("A");
		this.output.push();
		System.out.print("B");
		this.output.push();
		System.out.print("C");
		assertThat(this.output).isEqualTo("BC");
	}

	@Test
	void popWhenEmptyThrowsException() {
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(this.output::pop);
	}

	@Test
	void popWhenHasExistingEndsCapture() {
		this.output.push();
		System.out.print("A");
		this.output.pop();
		System.out.print("B");
		assertThat(this.systemOut.toString()).isEqualTo("AB");
	}

	@Test
	void captureAlsoWritesToSystemOut() {
		this.output.push();
		System.out.print("A");
		assertThat(this.systemOut.toString()).isEqualTo("A");
	}

	@Test
	void captureAlsoWritesToSystemErr() {
		this.output.push();
		System.err.print("A");
		assertThat(this.systemErr.toString()).isEqualTo("A");
	}

	@Test
	void lengthReturnsCapturedLength() {
		this.output.push();
		System.out.print("ABC");
		assertThat(this.output.length()).isEqualTo(3);
	}

	@Test
	void charAtReturnsCapturedCharAt() {
		this.output.push();
		System.out.print("ABC");
		assertThat(this.output.charAt(1)).isEqualTo('B');
	}

	@Test
	void subSequenceReturnsCapturedSubSequence() {
		this.output.push();
		System.out.print("ABC");
		assertThat(this.output.subSequence(1, 3)).isEqualTo("BC");
	}

	@Test
	void getAllReturnsAllCapturedOutput() {
		this.output.push();
		System.out.print("A");
		System.err.print("B");
		System.out.print("C");
		assertThat(this.output.getAll()).isEqualTo("ABC");
	}

	@Test
	void toStringReturnsAllCapturedOutput() {
		this.output.push();
		System.out.print("A");
		System.err.print("B");
		System.out.print("C");
		assertThat(this.output.toString()).isEqualTo("ABC");
	}

	@Test
	void getErrReturnsOnlyCapturedErrOutput() {
		this.output.push();
		System.out.print("A");
		System.err.print("B");
		System.out.print("C");
		assertThat(this.output.getErr()).isEqualTo("B");
	}

	@Test
	void getOutReturnsOnlyCapturedOutOutput() {
		this.output.push();
		System.out.print("A");
		System.err.print("B");
		System.out.print("C");
		assertThat(this.output.getOut()).isEqualTo("AC");
	}

	static class TestPrintStream extends PrintStream {

		TestPrintStream() {
			super(new ByteArrayOutputStream());
		}

		@Override
		public String toString() {
			return this.out.toString();
		}

	}

}
