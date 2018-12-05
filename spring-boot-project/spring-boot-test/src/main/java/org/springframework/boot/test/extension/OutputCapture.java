/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.boot.test.extension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import org.springframework.boot.ansi.AnsiOutput;

import static org.hamcrest.Matchers.allOf;

/**
 * JUnit5 {@code @Extension} to capture output from System.out and System.err.
 *
 * @author Madhura Bhave
 */
public class OutputCapture implements BeforeEachCallback, AfterEachCallback,
		BeforeAllCallback, ParameterResolver, CharSequence {

	private CaptureOutputStream captureOut;

	private CaptureOutputStream captureErr;

	private ByteArrayOutputStream methodLevelCopy;

	private ByteArrayOutputStream classLevelCopy;

	private List<Matcher<? super String>> matchers = new ArrayList<>();

	@Override
	public void afterEach(ExtensionContext context) {
		try {
			if (!this.matchers.isEmpty()) {
				String output = this.toString();
				Assert.assertThat(output, allOf(this.matchers));
			}
		}
		finally {
			releaseOutput();
		}

	}

	@Override
	public void beforeEach(ExtensionContext context) {
		releaseOutput();
		this.methodLevelCopy = new ByteArrayOutputStream();
		captureOutput(this.methodLevelCopy);
	}

	private void captureOutput(ByteArrayOutputStream copy) {
		AnsiOutputControl.get().disableAnsiOutput();
		this.captureOut = new CaptureOutputStream(System.out, copy);
		this.captureErr = new CaptureOutputStream(System.err, copy);
		System.setOut(new PrintStream(this.captureOut));
		System.setErr(new PrintStream(this.captureErr));
	}

	private void releaseOutput() {
		AnsiOutputControl.get().enabledAnsiOutput();
		System.setOut(this.captureOut.getOriginal());
		System.setErr(this.captureErr.getOriginal());
		this.methodLevelCopy = null;
	}

	private void flush() {
		try {
			this.captureOut.flush();
			this.captureErr.flush();
		}
		catch (IOException ex) {
			// ignore
		}
	}

	@Override
	public int length() {
		return this.toString().length();
	}

	@Override
	public char charAt(int index) {
		return this.toString().charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return this.toString().subSequence(start, end);
	}

	@Override
	public String toString() {
		flush();
		if (this.classLevelCopy == null && this.methodLevelCopy == null) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		if (this.classLevelCopy != null) {
			builder.append(this.classLevelCopy.toString());
		}
		builder.append(this.methodLevelCopy.toString());
		return builder.toString();
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		this.classLevelCopy = new ByteArrayOutputStream();
		captureOutput(this.classLevelCopy);
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		return OutputCapture.class.equals(parameterContext.getParameter().getType());
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		return this;
	}

	private static class CaptureOutputStream extends OutputStream {

		private final PrintStream original;

		private final OutputStream copy;

		CaptureOutputStream(PrintStream original, OutputStream copy) {
			this.original = original;
			this.copy = copy;
		}

		@Override
		public void write(int b) throws IOException {
			this.copy.write(b);
			this.original.write(b);
			this.original.flush();
		}

		@Override
		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			this.copy.write(b, off, len);
			this.original.write(b, off, len);
		}

		public PrintStream getOriginal() {
			return this.original;
		}

		@Override
		public void flush() throws IOException {
			this.copy.flush();
			this.original.flush();
		}

	}

	/**
	 * Allow AnsiOutput to not be on the test classpath.
	 */
	private static class AnsiOutputControl {

		public void disableAnsiOutput() {
		}

		public void enabledAnsiOutput() {
		}

		public static AnsiOutputControl get() {
			try {
				Class.forName("org.springframework.boot.ansi.AnsiOutput");
				return new AnsiPresentOutputControl();
			}
			catch (ClassNotFoundException ex) {
				return new AnsiOutputControl();
			}
		}

	}

	private static class AnsiPresentOutputControl extends AnsiOutputControl {

		@Override
		public void disableAnsiOutput() {
			AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		}

		@Override
		public void enabledAnsiOutput() {
			AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
		}

	}

}
