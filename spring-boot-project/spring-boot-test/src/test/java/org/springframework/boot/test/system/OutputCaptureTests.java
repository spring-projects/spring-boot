/*
 * Copyright 2012-2023 the original author or authors.
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
import java.util.function.Predicate;

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

	private final TestOutputCapture output = new TestOutputCapture();

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
		assertThat(this.systemOut).hasToString("AB");
	}

	@Test
	void captureAlsoWritesToSystemOut() {
		this.output.push();
		System.out.print("A");
		assertThat(this.systemOut).hasToString("A");
	}

	@Test
	void captureAlsoWritesToSystemErr() {
		this.output.push();
		System.err.print("A");
		assertThat(this.systemErr).hasToString("A");
	}

	@Test
	void lengthReturnsCapturedLength() {
		this.output.push();
		System.out.print("ABC");
		assertThat(this.output).hasSize(3);
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
		pushAndPrint();
		assertThat(this.output.getAll()).isEqualTo("ABC");
	}

	@Test
	void toStringReturnsAllCapturedOutput() {
		pushAndPrint();
		assertThat(this.output).hasToString("ABC");
	}

	@Test
	void getErrReturnsOnlyCapturedErrOutput() {
		pushAndPrint();
		assertThat(this.output.getErr()).isEqualTo("B");
	}

	@Test
	void getOutReturnsOnlyCapturedOutOutput() {
		pushAndPrint();
		assertThat(this.output.getOut()).isEqualTo("AC");
	}

	@Test
	void getAllUsesCache() {
		pushAndPrint();
		for (int i = 0; i < 10; i++) {
			assertThat(this.output.getAll()).isEqualTo("ABC");
		}
		assertThat(this.output.buildCount).isOne();
		System.out.print("X");
		assertThat(this.output.getAll()).isEqualTo("ABCX");
		assertThat(this.output.buildCount).isEqualTo(2);
	}

	@Test
	void getOutUsesCache() {
		pushAndPrint();
		for (int i = 0; i < 10; i++) {
			assertThat(this.output.getOut()).isEqualTo("AC");
		}
		assertThat(this.output.buildCount).isOne();
		System.out.print("X");
		assertThat(this.output.getOut()).isEqualTo("ACX");
		assertThat(this.output.buildCount).isEqualTo(2);
	}

	@Test
	void getErrUsesCache() {
		pushAndPrint();
		for (int i = 0; i < 10; i++) {
			assertThat(this.output.getErr()).isEqualTo("B");
		}
		assertThat(this.output.buildCount).isOne();
		System.err.print("X");
		assertThat(this.output.getErr()).isEqualTo("BX");
		assertThat(this.output.buildCount).isEqualTo(2);
	}

	private void pushAndPrint() {
		this.output.push();
		System.out.print("A");
		System.err.print("B");
		System.out.print("C");
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

	static class TestOutputCapture extends OutputCapture {

		int buildCount;

		@Override
		String build(Predicate<Type> filter) {
			this.buildCount++;
			return super.build(filter);
		}

	}

}
