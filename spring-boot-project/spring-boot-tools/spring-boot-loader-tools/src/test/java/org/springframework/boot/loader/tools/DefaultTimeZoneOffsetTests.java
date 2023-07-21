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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultTimeZoneOffset}
 *
 * @author Phillip Webb
 */
class DefaultTimeZoneOffsetTests {

	// gh-34424

	@Test
	void removeFromWithLongInDifferentTimeZonesReturnsSameValue() {
		long time = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli();
		TimeZone timeZone1 = TimeZone.getTimeZone("GMT");
		TimeZone timeZone2 = TimeZone.getTimeZone("GMT+8");
		TimeZone timeZone3 = TimeZone.getTimeZone("GMT-8");
		long result1 = new DefaultTimeZoneOffset(timeZone1).removeFrom(time);
		long result2 = new DefaultTimeZoneOffset(timeZone2).removeFrom(time);
		long result3 = new DefaultTimeZoneOffset(timeZone3).removeFrom(time);
		long dosTime1 = toDosTime(Calendar.getInstance(timeZone1), result1);
		long dosTime2 = toDosTime(Calendar.getInstance(timeZone2), result2);
		long dosTime3 = toDosTime(Calendar.getInstance(timeZone3), result3);
		assertThat(dosTime1).isEqualTo(dosTime2).isEqualTo(dosTime3);
	}

	@Test
	void removeFromWithFileTimeReturnsFileTime() {
		long time = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli();
		long result = new DefaultTimeZoneOffset(TimeZone.getTimeZone("GMT+8")).removeFrom(time);
		assertThat(result).isNotEqualTo(time).isEqualTo(946656000000L);
	}

	/**
	 * Identical functionality to package-private
	 * org.apache.commons.compress.archivers.zip.ZipUtil.toDosTime(Calendar, long, byte[],
	 * int) method used by {@link ZipArchiveOutputStream} to convert times.
	 * @param calendar the source calendar
	 * @param time the time to convert
	 * @return the DOS time
	 */
	private long toDosTime(Calendar calendar, long time) {
		calendar.setTimeInMillis(time);
		final int year = calendar.get(Calendar.YEAR);
		final int month = calendar.get(Calendar.MONTH) + 1;
		return ((year - 1980) << 25) | (month << 21) | (calendar.get(Calendar.DAY_OF_MONTH) << 16)
				| (calendar.get(Calendar.HOUR_OF_DAY) << 11) | (calendar.get(Calendar.MINUTE) << 5)
				| (calendar.get(Calendar.SECOND) >> 1);
	}

}
