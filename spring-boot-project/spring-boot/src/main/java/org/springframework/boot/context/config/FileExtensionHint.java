/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.context.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User provided hint for an otherwise missing file extension.
 *
 * @author Phillip Webb
 */
final class FileExtensionHint {

	private static final Pattern PATTERN = Pattern.compile("^(.*)\\[(\\.\\w+)](?!\\[)$");

	private static final FileExtensionHint NONE = new FileExtensionHint(null);

	private final Matcher matcher;

	private FileExtensionHint(Matcher matcher) {
		this.matcher = matcher;
	}

	/**
	 * Return {@code true} if the hint is present.
	 * @return if the hint is present
	 */
	boolean isPresent() {
		return this.matcher != null;
	}

	/**
	 * Return the extension from the hint or return the parameter if the hint is not
	 * {@link #isPresent() present}.
	 * @param extension the fallback extension
	 * @return the extension either from the hint or fallback
	 */
	String orElse(String extension) {
		return (this.matcher != null) ? toString() : extension;
	}

	@Override
	public String toString() {
		return (this.matcher != null) ? this.matcher.group(2) : "";
	}

	/**
	 * Return the {@link FileExtensionHint} from the given value.
	 * @param value the source value
	 * @return the {@link FileExtensionHint} (never {@code null})
	 */
	static FileExtensionHint from(String value) {
		Matcher matcher = PATTERN.matcher(value);
		return (matcher.matches()) ? new FileExtensionHint(matcher) : NONE;
	}

	/**
	 * Remove any hint from the given value.
	 * @param value the source value
	 * @return the value without any hint
	 */
	static String removeFrom(String value) {
		Matcher matcher = PATTERN.matcher(value);
		return (matcher.matches()) ? matcher.group(1) : value;
	}

}
