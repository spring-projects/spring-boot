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

package org.springframework.boot.loader.tools;

import java.nio.file.attribute.FileTime;
import java.util.TimeZone;
import java.util.zip.ZipEntry;

/**
 * Utility class that can be used change a UTC time based on the
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
