/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * User-provided hint for an otherwise missing file extension.
 *
 * @author Phillip Webb
 */
final class FileExtensionHint {

	private static final Pattern PATTERN = Pattern.compile("^(.*)\\[(\\.\\w+)](?!\\[)$");

	private static final FileExtensionHint NONE = new FileExtensionHint(null);

	private final @Nullable Matcher matcher;

	private FileExtensionHint(@Nullable Matcher matcher) {
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
