/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import java.nio.file.attribute.FileTime;
import java.util.TimeZone;
import java.util.zip.ZipEntry;

/**
 * Utility class that can be used to change a UTC time based on the
 * {@link java.util.TimeZone#getDefault() default TimeZone}. This is required because
 * {@link ZipEntry#setTime(long)} expects times in the default timezone and not UTC.
 *
 * @author Phillip Webb
 */
class DefaultTimeZoneOffset {

	static final DefaultTimeZoneOffset INSTANCE = new DefaultTimeZoneOffset(TimeZone.getDefault());

	private final TimeZone defaultTimeZone;

	DefaultTimeZoneOffset(TimeZone defaultTimeZone) {
		this.defaultTimeZone = defaultTimeZone;
	}

	/**
	 * Remove the default offset from the given time.
	 * @param time the time to remove the default offset from
	 * @return the time with the default offset removed
	 */
	FileTime removeFrom(FileTime time) {
		return FileTime.fromMillis(removeFrom(time.toMillis()));
	}

	/**
	 * Remove the default offset from the given time.
	 * @param time the time to remove the default offset from
	 * @return the time with the default offset removed
	 */
	long removeFrom(long time) {
		return time - this.defaultTimeZone.getOffset(time);
	}

}
