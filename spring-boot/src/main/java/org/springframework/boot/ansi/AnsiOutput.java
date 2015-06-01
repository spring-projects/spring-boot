/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.ansi;

import org.springframework.util.Assert;

/**
 * Generates ANSI encoded output, automatically attempting to detect if the terminal
 * supports ANSI.
 *
 * @author Phillip Webb
 */
public abstract class AnsiOutput {

	private static final String ENCODE_JOIN = ";";

	private static Enabled enabled = Enabled.DETECT;

	private static final String OPERATING_SYSTEM_NAME = System.getProperty("os.name")
			.toLowerCase();

	private static final String ENCODE_START = "\033[";

	private static final String ENCODE_END = "m";

	private static final String RESET = "0;" + AnsiElement.DEFAULT;

	/**
	 * Sets if ANSI output is enabled.
	 * @param enabled if ANSI is enabled, disabled or detected
	 */
	public static void setEnabled(Enabled enabled) {
		Assert.notNull(enabled, "Enabled must not be null");
		AnsiOutput.enabled = enabled;
	}

	/**
	 * Create a new ANSI string from the specified elements. Any {@link AnsiElement}s will
	 * be encoded as required.
	 * @param elements the elements to encode
	 * @return a string of the encoded elements
	 */
	public static String toString(Object... elements) {
		StringBuilder sb = new StringBuilder();
		if (isEnabled()) {
			buildEnabled(sb, elements);
		}
		else {
			buildDisabled(sb, elements);
		}
		return sb.toString();
	}

	private static void buildEnabled(StringBuilder sb, Object[] elements) {
		boolean writingAnsi = false;
		boolean containsEncoding = false;
		for (Object element : elements) {
			if (element instanceof AnsiElement) {
				containsEncoding = true;
				if (!writingAnsi) {
					sb.append(ENCODE_START);
					writingAnsi = true;
				}
				else {
					sb.append(ENCODE_JOIN);
				}
			}
			else {
				if (writingAnsi) {
					sb.append(ENCODE_END);
					writingAnsi = false;
				}
			}
			sb.append(element);
		}
		if (containsEncoding) {
			sb.append(writingAnsi ? ENCODE_JOIN : ENCODE_START);
			sb.append(RESET);
			sb.append(ENCODE_END);
		}
	}

	private static void buildDisabled(StringBuilder sb, Object[] elements) {
		for (Object element : elements) {
			if (!(element instanceof AnsiElement) && element != null) {
				sb.append(element);
			}
		}
	}

	private static boolean isEnabled() {
		if (enabled == Enabled.DETECT) {
			return detectIfEnabled();
		}
		return enabled == Enabled.ALWAYS;
	}

	private static boolean detectIfEnabled() {
		try {
			if (System.console() == null) {
				return false;
			}
			return !(OPERATING_SYSTEM_NAME.indexOf("win") >= 0);
		}
		catch (Throwable ex) {
			return false;
		}
	}

	public static enum Enabled {
		DETECT, ALWAYS, NEVER
	};

}
