/*
 * Copyright 2002-2020 the original author or authors.
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
 * @since 2.3.0
 * @see Period
 */
public enum PeriodStyle {

	/**
	 * Simple formatting, for example '1d'.
	 */
	SIMPLE("^" + "(?:([-+]?[0-9]+)Y)?" + "(?:([-+]?[0-9]+)M)?" + "(?:([-+]?[0-9]+)W)?" + "(?:([-+]?[0-9]+)D)?" + "$",
			Pattern.CASE_INSENSITIVE) {

		@Override
		public Period parse(String value, ChronoUnit unit) {
			try {
				if (NUMERIC.matcher(value).matches()) {
					return Unit.fromChronoUnit(unit).parse(value);
				}
				Matcher matcher = matcher(value);
				Assert.state(matcher.matches(), "Does not match simple period pattern");
				Assert.isTrue(hasAtLeastOneGroupValue(matcher), "'" + value + "' is not a valid simple period");
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

		boolean hasAtLeastOneGroupValue(Matcher matcher) {
			for (int i = 0; i < matcher.groupCount(); i++) {
				if (matcher.group(i + 1) != null) {
					return true;
				}
			}
			return false;
		}

		private int parseInt(Matcher matcher, int group) {
			String value = matcher.group(group);
			return (value != null) ? Integer.parseInt(value) : 0;
		}

		@Override
		protected boolean matches(String value) {
			return NUMERIC.matcher(value).matches() || matcher(value).matches();
		}

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

		private void append(StringBuilder result, Period value, Unit unit) {
			if (!unit.isZero(value)) {
				result.append(unit.print(value));
			}
		}

	},

	/**
	 * ISO-8601 formatting.
	 */
	ISO8601("^[+-]?P.*$", 0) {

		@Override
		public Period parse(String value, ChronoUnit unit) {
			try {
				return Period.parse(value);
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("'" + value + "' is not a valid ISO-8601 period", ex);
			}
		}

		@Override
		public String print(Period value, ChronoUnit unit) {
			return value.toString();
		}

	};

	private static final Pattern NUMERIC = Pattern.compile("^[-+]?[0-9]+$");

	private final Pattern pattern;

	PeriodStyle(String pattern, int flags) {
		this.pattern = Pattern.compile(pattern, flags);
	}

	protected boolean matches(String value) {
		return this.pattern.matcher(value).matches();
	}

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

	enum Unit {

		/**
		 * Days, represented by suffix {@code d}.
		 */
		DAYS(ChronoUnit.DAYS, "d", Period::getDays, Period::ofDays),

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

		Unit(ChronoUnit chronoUnit, String suffix, Function<Period, Integer> intValue,
				Function<Integer, Period> factory) {
			this.chronoUnit = chronoUnit;
			this.suffix = suffix;
			this.intValue = intValue;
			this.factory = factory;
		}

		private Period parse(String value) {
			return this.factory.apply(Integer.parseInt(value));
		}

		private String print(Period value) {
			return intValue(value) + this.suffix;
		}

		public boolean isZero(Period value) {
			return intValue(value) == 0;
		}

		public int intValue(Period value) {
			return this.intValue.apply(value);
		}

		public static Unit fromChronoUnit(ChronoUnit chronoUnit) {
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
