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

package org.springframework.boot.buildpack.platform.docker;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TotalProgressBar}.
 *
 * @author Phillip Webb
 */
class TotalProgressBarTests {

	@Test
	void withPrefixAndBookends() {
		TestPrintStream out = new TestPrintStream();
		TotalProgressBar bar = new TotalProgressBar("prefix:", '#', true, out);
		assertThat(out).hasToString("prefix: [ ");
		bar.accept(new TotalProgressEvent(10));
		assertThat(out).hasToString("prefix: [ #####");
		bar.accept(new TotalProgressEvent(50));
		assertThat(out).hasToString("prefix: [ #########################");
		bar.accept(new TotalProgressEvent(100));
		assertThat(out).hasToString(String.format("prefix: [ ################################################## ]%n"));
	}

	@Test
	void withoutPrefix() {
		TestPrintStream out = new TestPrintStream();
		TotalProgressBar bar = new TotalProgressBar(null, '#', true, out);
		assertThat(out).hasToString("[ ");
		bar.accept(new TotalProgressEvent(10));
		assertThat(out).hasToString("[ #####");
		bar.accept(new TotalProgressEvent(50));
		assertThat(out).hasToString("[ #########################");
		bar.accept(new TotalProgressEvent(100));
		assertThat(out).hasToString(String.format("[ ################################################## ]%n"));
	}

	@Test
	void withoutBookends() {
		TestPrintStream out = new TestPrintStream();
		TotalProgressBar bar = new TotalProgressBar("", '.', false, out);
		assertThat(out).hasToString("");
		bar.accept(new TotalProgressEvent(10));
		assertThat(out).hasToString(".....");
		bar.accept(new TotalProgressEvent(50));
		assertThat(out).hasToString(".........................");
		bar.accept(new TotalProgressEvent(100));
		assertThat(out).hasToString(String.format("..................................................%n"));
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
