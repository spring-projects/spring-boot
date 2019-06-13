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

/**
 * Provides access to {@link System#out System.out} and {@link System#err System.err}
 * output that has been captured by the {@link OutputCaptureExtension}. Can be used to
 * apply assertions either using AssertJ or standard JUnit assertions. For example:
 * <pre class="code">
 * assertThat(output).contains("started"); // Checks all output
 * assertThat(output.getErr()).contains("failed"); // Only checks System.err
 * assertThat(output.getOut()).contains("ok"); // Only checks System.put
 * </pre>
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.2.0
 * @see OutputCaptureExtension
 */
public interface CapturedOutput extends CharSequence {

	@Override
	default int length() {
		return toString().length();
	}

	@Override
	default char charAt(int index) {
		return toString().charAt(index);
	}

	@Override
	default CharSequence subSequence(int start, int end) {
		return toString().subSequence(start, end);
	}

	/**
	 * Return all content (both {@link System#out System.out} and {@link System#err
	 * System.err}) in the order that it was was captured.
	 * @return all captured output
	 */
	String getAll();

	/**
	 * Return {@link System#out System.out} content in the order that it was was captured.
	 * @return {@link System#out System.out} captured output
	 */
	String getOut();

	/**
	 * Return {@link System#err System.err} content in the order that it was was captured.
	 * @return {@link System#err System.err} captured output
	 */
	String getErr();

}
