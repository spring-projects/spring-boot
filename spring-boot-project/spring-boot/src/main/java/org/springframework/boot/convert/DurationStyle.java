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

package org.springframework.boot.convert;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Duration format styles.
 *
 * @author Phillip Webb
 * @author Valentine Wu
 * @since 2.0.0
 */
public enum DurationStyle {

	/**
	 * Simple formatting, for example '1s'.
	 */
	SIMPLE("^([+-]?\\d+)([a-zA-Z]{0,2})$") {

		/**
		 * Parses a string value into a Duration object using the specified ChronoUnit.
		 * @param value the string value to be parsed
		 * @param unit the ChronoUnit to be used for parsing
		 * @return the parsed Duration object
		 * @throws IllegalArgumentException if the value is not a valid simple duration
		 */
		@Override
		public Duration parse(String value, ChronoUnit unit) {
			try {
				Matcher matcher = matcher(value);
				Assert.state(matcher.matches(), "Does not match simple duration pattern");
				String suffix = matcher.group(2);
				return (StringUtils.hasLength(suffix) ? Unit.fromSuffix(suffix) : Unit.fromChronoUnit(unit))
					.parse(matcher.group(1));
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("'" + value + "' is not a valid simple duration", ex);
			}
		}

		/**
		 * Prints the given duration value in the specified unit.
		 * @param value the duration value to be printed
		 * @param unit the unit in which the duration value should be printed
		 * @return the string representation of the duration value in the specified unit
		 */
		@Override
		public String print(Duration value, ChronoUnit unit) {
			return Unit.fromChronoUnit(unit).print(value);
		}

	},

	/**
	 * ISO-8601 formatting.
	 */
	ISO8601("^[+-]?[pP].*$") {

		/**
		 * Parses a string representation of a duration in ISO-8601 format and returns a
		 * Duration object.
		 * @param value the string representation of the duration
		 * @param unit the unit of the duration (e.g. ChronoUnit.SECONDS,
		 * ChronoUnit.MINUTES, etc.)
		 * @return the parsed Duration object
		 * @throws IllegalArgumentException if the input string is not a valid ISO-8601
		 * duration
		 */
		@Override
		public Duration parse(String value, ChronoUnit unit) {
			try {
				return Duration.parse(value);
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("'" + value + "' is not a valid ISO-8601 duration", ex);
			}
		}

		/**
		 * Returns a string representation of the given duration value in the specified
		 * unit.
		 * @param value the duration value to be printed
		 * @param unit the unit in which the duration value is to be printed
		 * @return a string representation of the duration value in the specified unit
		 */
		@Override
		public String print(Duration value, ChronoUnit unit) {
			return value.toString();
		}

	};

	private final Pattern pattern;

	/**
	 * Constructs a new DurationStyle object with the specified pattern.
	 * @param pattern the pattern used to create the DurationStyle object
	 */
	DurationStyle(String pattern) {
		this.pattern = Pattern.compile(pattern);
	}

	/**
	 * Checks if the given value matches the pattern of this DurationStyle.
	 * @param value the value to be checked
	 * @return true if the value matches the pattern, false otherwise
	 */
	protected final boolean matches(String value) {
		return this.pattern.matcher(value).matches();
	}

	/**
	 * Returns a Matcher object that matches the given value against the pattern of this
	 * DurationStyle object.
	 * @param value the value to be matched against the pattern
	 * @return a Matcher object that matches the given value against the pattern of this
	 * DurationStyle object
	 */
	protected final Matcher matcher(String value) {
		return this.pattern.matcher(value);
	}

	/**
	 * Parse the given value to a duration.
	 * @param value the value to parse
	 * @return a duration
	 */
	public Duration parse(String value) {
		return parse(value, null);
	}

	/**
	 * Parse the given value to a duration.
	 * @param value the value to parse
	 * @param unit the duration unit to use if the value doesn't specify one ({@code null}
	 * will default to ms)
	 * @return a duration
	 */
	public abstract Duration parse(String value, ChronoUnit unit);

	/**
	 * Print the specified duration.
	 * @param value the value to print
	 * @return the printed result
	 */
	public String print(Duration value) {
		return print(value, null);
	}

	/**
	 * Print the specified duration using the given unit.
	 * @param value the value to print
	 * @param unit the value to use for printing
	 * @return the printed result
	 */
	public abstract String print(Duration value, ChronoUnit unit);

	/**
	 * Detect the style then parse the value to return a duration.
	 * @param value the value to parse
	 * @return the parsed duration
	 * @throws IllegalArgumentException if the value is not a known style or cannot be
	 * parsed
	 */
	public static Duration detectAndParse(String value) {
		return detectAndParse(value, null);
	}

	/**
	 * Detect the style then parse the value to return a duration.
	 * @param value the value to parse
	 * @param unit the duration unit to use if the value doesn't specify one ({@code null}
	 * will default to ms)
	 * @return the parsed duration
	 * @throws IllegalArgumentException if the value is not a known style or cannot be
	 * parsed
	 */
	public static Duration detectAndParse(String value, ChronoUnit unit) {
		return detect(value).parse(value, unit);
	}

	/**
	 * Detect the style from the given source value.
	 * @param value the source value
	 * @return the duration style
	 * @throws IllegalArgumentException if the value is not a known style
	 */
	public static DurationStyle detect(String value) {
		Assert.notNull(value, "Value must not be null");
		for (DurationStyle candidate : values()) {
			if (candidate.matches(value)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException("'" + value + "' is not a valid duration");
	}

	/**
	 * Units that we support.
	 */
	enum Unit {

		/**
		 * Nanoseconds.
		 */
		NANOS(ChronoUnit.NANOS, "ns", Duration::toNanos),

		/**
		 * Microseconds.
		 */
		MICROS(ChronoUnit.MICROS, "us", (duration) -> duration.toNanos() / 1000L),

		/**
		 * Milliseconds.
		 */
		MILLIS(ChronoUnit.MILLIS, "ms", Duration::toMillis),

		/**
		 * Seconds.
		 */
		SECONDS(ChronoUnit.SECONDS, "s", Duration::getSeconds),

		/**
		 * Minutes.
		 */
		MINUTES(ChronoUnit.MINUTES, "m", Duration::toMinutes),

		/**
		 * Hours.
		 */
		HOURS(ChronoUnit.HOURS, "h", Duration::toHours),

		/**
		 * Days.
		 */
		DAYS(ChronoUnit.DAYS, "d", Duration::toDays);

		private final ChronoUnit chronoUnit;

		private final String suffix;

		private final Function<Duration, Long> longValue;

		/**
		 * Constructs a new DurationStyle unit with the specified ChronoUnit, suffix, and
		 * conversion function.
		 * @param chronoUnit the ChronoUnit representing the duration unit
		 * @param suffix the suffix to be appended to the duration value
		 * @param toUnit the conversion function to convert the duration to a long value
		 */
		Unit(ChronoUnit chronoUnit, String suffix, Function<Duration, Long> toUnit) {
			this.chronoUnit = chronoUnit;
			this.suffix = suffix;
			this.longValue = toUnit;
		}

		/**
		 * Parses the given value and returns a Duration object.
		 * @param value the value to be parsed
		 * @return the parsed Duration object
		 * @throws NumberFormatException if the value cannot be parsed as a long
		 */
		public Duration parse(String value) {
			return Duration.of(Long.parseLong(value), this.chronoUnit);
		}

		/**
		 * Prints the value of the given duration with the suffix.
		 * @param value the duration value to be printed
		 * @return the string representation of the duration value with the suffix
		 */
		public String print(Duration value) {
			return longValue(value) + this.suffix;
		}

		/**
		 * Converts a Duration value to a long value.
		 * @param value the Duration value to be converted
		 * @return the long value representation of the Duration value
		 */
		public long longValue(Duration value) {
			return this.longValue.apply(value);
		}

		/**
		 * Converts a ChronoUnit to a Unit.
		 * @param chronoUnit the ChronoUnit to convert
		 * @return the corresponding Unit
		 * @throws IllegalArgumentException if the ChronoUnit is unknown
		 */
		public static Unit fromChronoUnit(ChronoUnit chronoUnit) {
			if (chronoUnit == null) {
				return Unit.MILLIS;
			}
			for (Unit candidate : values()) {
				if (candidate.chronoUnit == chronoUnit) {
					return candidate;
				}
			}
			throw new IllegalArgumentException("Unknown unit " + chronoUnit);
		}

		/**
		 * Returns the Unit enum constant that corresponds to the given suffix.
		 * @param suffix the suffix to match with a Unit enum constant
		 * @return the Unit enum constant that matches the given suffix
		 * @throws IllegalArgumentException if no Unit enum constant matches the given
		 * suffix
		 */
		public static Unit fromSuffix(String suffix) {
			for (Unit candidate : values()) {
				if (candidate.suffix.equalsIgnoreCase(suffix)) {
					return candidate;
				}
			}
			throw new IllegalArgumentException("Unknown unit '" + suffix + "'");
		}

	}

}
