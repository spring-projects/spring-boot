/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot;

import java.time.Duration;

/**
 * Format styles for displaying application startup time in logs.
 *
 * @author Huang Xiao
 * @since 3.5.0
 */
public enum StartupTimeFormat {

	/**
	 * Default format displays time in seconds with millisecond precision (e.g., "3.456
	 * seconds"). This maintains backward compatibility with existing log parsing tools.
	 */
	DEFAULT {

		@Override
		public String format(long millis) {
			return String.format("%.3f seconds", millis / 1000.0);
		}

	},

	/**
	 * Human format displays time in a more intuitive way using appropriate units (e.g.,
	 * "1 minute 30 seconds" or "1 hour 15 minutes"). Times under 60 seconds still use the
	 * default format for consistency.
	 */
	HUMAN {

		@Override
		public String format(long millis) {
			Duration duration = Duration.ofMillis(millis);
			long seconds = duration.getSeconds();
			if (seconds < 60) {
				return String.format("%.3f seconds", millis / 1000.0);
			}
			long hours = duration.toHours();
			int minutes = duration.toMinutesPart();
			int secs = duration.toSecondsPart();
			if (hours > 0) {
				return String.format("%d hour%s %d minute%s", hours, (hours != 1) ? "s" : "", minutes,
						(minutes != 1) ? "s" : "");
			}
			return String.format("%d minute%s %d second%s", minutes, (minutes != 1) ? "s" : "", secs,
					(secs != 1) ? "s" : "");
		}

	};

	/**
	 * Format the given duration in milliseconds according to this format style.
	 * @param millis the duration in milliseconds
	 * @return the formatted string
	 */
	public abstract String format(long millis);

}
