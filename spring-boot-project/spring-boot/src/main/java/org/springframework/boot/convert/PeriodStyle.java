/*
 * Copyright 2002-2022 the original author or authors.
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

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * A standard set of {@link Period} units.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 * @author Valentine Wu
 * @since 2.3.0
 * @see Period
 */
public enum PeriodStyle {

	/**
	 * Simple formatting, for example '1d'.
	 */
	SIMPLE("^" + "(?:([-+]?[0-9]+)Y)?" + "(?:([-+]?[0-9]+)M)?" + "(?:([-+]?[0-9]+)W)?" + "(?:([-+]?[0-9]+)D)?" + "$",
			Pattern.CASE_INSENSITIVE) {

		/**
		 * Parses a string value into a Period object based on the provided ChronoUnit.
		 * @param value the string value to parse
		 * @param unit the ChronoUnit to use for parsing
		 * @return the parsed Period object
		 * @throws IllegalArgumentException if the value is not a valid simple period
		 */
		@Override
		public Period parse(String value, ChronoUnit unit) {
			try {
				if (NUMERIC.matcher(value).matches()) {
					return Unit.fromChronoUnit(unit).parse(value);
				}
				Matcher matcher = matcher(value);
				Assert.state(matcher.matches(), "Does not match simple period pattern");
				Assert.isTrue(hasAtLeastOneGroupValue(matcher), () -> "'" + value + "' is not a valid simple period");
				int years = parseInt(matcher, 1);
				int months = parseInt(matcher, 2);
				int weeks = parseInt(matcher, 3);
				int days = parseInt(matcher, 4);
				return Period.of(years, months, Math.addExact(Math.multiplyExact(weeks, 7), days));
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("'" + value + "' is not a valid simple period", ex);
			}
		}

		/**
		 * Checks if the given Matcher object has at least one group value.
		 * @param matcher the Matcher object to check
		 * @return true if the Matcher object has at least one group value, false
		 * otherwise
		 */
		boolean hasAtLeastOneGroupValue(Matcher matcher) {
			for (int i = 0; i < matcher.groupCount(); i++) {
				if (matcher.group(i + 1) != null) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Parses the integer value from the specified group in the given Matcher object.
		 * @param matcher the Matcher object containing the matched string
		 * @param group the group index from which to extract the integer value
		 * @return the parsed integer value, or 0 if the value is null
		 */
		private int parseInt(Matcher matcher, int group) {
			String value = matcher.group(group);
			return (value != null) ? Integer.parseInt(value) : 0;
		}

		/**
		 * Determines if the given value matches the specified pattern.
		 * @param value the value to be checked
		 * @return true if the value matches the pattern, false otherwise
		 */
		@Override
		protected boolean matches(String value) {
			return NUMERIC.matcher(value).matches() || matcher(value).matches();
		}

		/**
		 * Prints the given period value in the specified unit.
		 * @param value the period value to be printed
		 * @param unit the unit in which the value should be printed
		 * @return the string representation of the period value in the specified unit
		 */
		@Override
		public String print(Period value, ChronoUnit unit) {
			if (value.isZero()) {
				return Unit.fromChronoUnit(unit).print(value);
			}
			StringBuilder result = new StringBuilder();
			append(result, value, Unit.YEARS);
			append(result, value, Unit.MONTHS);
			append(result, value, Unit.DAYS);
			return result.toString();
		}

		/**
		 * Appends the formatted value of a Period object to a StringBuilder, based on the
		 * specified unit.
		 * @param result the StringBuilder object to append the formatted value to
		 * @param value the Period object to format and append
		 * @param unit the Unit object representing the desired format for the value
		 */
		private void append(StringBuilder result, Period value, Unit unit) {
			if (!unit.isZero(value)) {
				result.append(unit.print(value));
			}
		}

	},

	/**
	 * ISO-8601 formatting.
	 */
	ISO8601("^[+-]?P.*$", Pattern.CASE_INSENSITIVE) {

		/**
		 * Parses a string representation of a period in ISO-8601 format and returns a
		 * {@link Period} object.
		 * @param value the string representation of the period
		 * @param unit the unit of the period (e.g. years, months, days)
		 * @return the parsed {@link Period} object
		 * @throws IllegalArgumentException if the input string is not a valid ISO-8601
		 * period
		 */
		@Override
		public Period parse(String value, ChronoUnit unit) {
			try {
				return Period.parse(value);
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("'" + value + "' is not a valid ISO-8601 period", ex);
			}
		}

		/**
		 * Returns a string representation of the given Period value in the specified
		 * ChronoUnit.
		 * @param value the Period value to be printed
		 * @param unit the ChronoUnit to be used for printing
		 * @return a string representation of the given Period value
		 */
		@Override
		public String print(Period value, ChronoUnit unit) {
			return value.toString();
		}

	};

	private static final Pattern NUMERIC = Pattern.compile("^[-+]?[0-9]+$");

	private final Pattern pattern;

	/**
	 * Constructs a new PeriodStyle object with the specified pattern and flags.
	 * @param pattern the regular expression pattern to be compiled
	 * @param flags the flags to be used for compiling the pattern
	 */
	PeriodStyle(String pattern, int flags) {
		this.pattern = Pattern.compile(pattern, flags);
	}

	/**
	 * Checks if the given value matches the pattern.
	 * @param value the value to be checked
	 * @return true if the value matches the pattern, false otherwise
	 */
	protected boolean matches(String value) {
		return this.pattern.matcher(value).matches();
	}

	/**
	 * Returns a Matcher object that matches the given value against the pattern of this
	 * PeriodStyle object.
	 * @param value the value to be matched against the pattern
	 * @return a Matcher object that matches the given value against the pattern
	 */
	protected final Matcher matcher(String value) {
		return this.pattern.matcher(value);
	}

	/**
	 * Parse the given value to a Period.
	 * @param value the value to parse
	 * @return a period
	 */
	public Period parse(String value) {
		return parse(value, null);
	}

	/**
	 * Parse the given value to a period.
	 * @param value the value to parse
	 * @param unit the period unit to use if the value doesn't specify one ({@code null}
	 * will default to d)
	 * @return a period
	 */
	public abstract Period parse(String value, ChronoUnit unit);

	/**
	 * Print the specified period.
	 * @param value the value to print
	 * @return the printed result
	 */
	public String print(Period value) {
		return print(value, null);
	}

	/**
	 * Print the specified period using the given unit.
	 * @param value the value to print
	 * @param unit the value to use for printing
	 * @return the printed result
	 */
	public abstract String print(Period value, ChronoUnit unit);

	/**
	 * Detect the style then parse the value to return a period.
	 * @param value the value to parse
	 * @return the parsed period
	 * @throws IllegalArgumentException if the value is not a known style or cannot be
	 * parsed
	 */
	public static Period detectAndParse(String value) {
		return detectAndParse(value, null);
	}

	/**
	 * Detect the style then parse the value to return a period.
	 * @param value the value to parse
	 * @param unit the period unit to use if the value doesn't specify one ({@code null}
	 * will default to ms)
	 * @return the parsed period
	 * @throws IllegalArgumentException if the value is not a known style or cannot be
	 * parsed
	 */
	public static Period detectAndParse(String value, ChronoUnit unit) {
		return detect(value).parse(value, unit);
	}

	/**
	 * Detect the style from the given source value.
	 * @param value the source value
	 * @return the period style
	 * @throws IllegalArgumentException if the value is not a known style
	 */
	public static PeriodStyle detect(String value) {
		Assert.notNull(value, "Value must not be null");
		for (PeriodStyle candidate : values()) {
			if (candidate.matches(value)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException("'" + value + "' is not a valid period");
	}

	private enum Unit {

		/**
		 * Days, represented by suffix {@code d}.
		 */
		DAYS(ChronoUnit.DAYS, "d", Period::getDays, Period::ofDays),

		/**
		 * Weeks, represented by suffix {@code w}.
		 */
		WEEKS(ChronoUnit.WEEKS, "w", null, Period::ofWeeks),

		/**
		 * Months, represented by suffix {@code m}.
		 */
		MONTHS(ChronoUnit.MONTHS, "m", Period::getMonths, Period::ofMonths),

		/**
		 * Years, represented by suffix {@code y}.
		 */
		YEARS(ChronoUnit.YEARS, "y", Period::getYears, Period::ofYears);

		private final ChronoUnit chronoUnit;

		private final String suffix;

		private final Function<Period, Integer> intValue;

		private final Function<Integer, Period> factory;

		/**
		 * Constructs a new Unit with the specified ChronoUnit, suffix, intValue function,
		 * and factory function.
		 * @param chronoUnit the ChronoUnit associated with this Unit
		 * @param suffix the suffix used to represent this Unit
		 * @param intValue the function used to convert a Period to an integer value for
		 * this Unit
		 * @param factory the function used to create a Period from an integer value for
		 * this Unit
		 */
		Unit(ChronoUnit chronoUnit, String suffix, Function<Period, Integer> intValue,
				Function<Integer, Period> factory) {
			this.chronoUnit = chronoUnit;
			this.suffix = suffix;
			this.intValue = intValue;
			this.factory = factory;
		}

		/**
		 * Parses a string value into a Period object.
		 * @param value the string value to be parsed
		 * @return the parsed Period object
		 */
		private Period parse(String value) {
			return this.factory.apply(Integer.parseInt(value));
		}

		/**
		 * Prints the value of the given Period object with the suffix appended.
		 * @param value the Period object to be printed
		 * @return the string representation of the value with the suffix appended
		 */
		private String print(Period value) {
			return intValue(value) + this.suffix;
		}

		/**
		 * Checks if the given Period value is equal to zero.
		 * @param value the Period value to be checked
		 * @return true if the value is equal to zero, false otherwise
		 */
		private boolean isZero(Period value) {
			return intValue(value) == 0;
		}

		/**
		 * Returns the integer value extracted from the given Period value.
		 * @param value the Period value from which the integer value is to be extracted
		 * @return the integer value extracted from the given Period value
		 * @throws IllegalArgumentException if the intValue is null
		 */
		private int intValue(Period value) {
			Assert.notNull(this.intValue, () -> "intValue cannot be extracted from " + name());
			return this.intValue.apply(value);
		}

		/**
		 * Converts a ChronoUnit to a Unit.
		 * @param chronoUnit the ChronoUnit to be converted
		 * @return the corresponding Unit
		 * @throws IllegalArgumentException if the ChronoUnit is not supported
		 */
		private static Unit fromChronoUnit(ChronoUnit chronoUnit) {
			if (chronoUnit == null) {
				return Unit.DAYS;
			}
			for (Unit candidate : values()) {
				if (candidate.chronoUnit == chronoUnit) {
					return candidate;
				}
			}
			throw new IllegalArgumentException("Unsupported unit " + chronoUnit);
		}

	}

}
