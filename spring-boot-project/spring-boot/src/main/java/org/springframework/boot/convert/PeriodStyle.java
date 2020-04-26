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

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A standard set of {@link DataSize} units.
 *
 * <p>
 * The unit prefixes used in this class are
 * <a href="https://en.wikipedia.org/wiki/Binary_prefix">binary prefixes</a> indicating
 * multiplication by powers of 2. The following table displays the enum constants defined
 * in this class and corresponding values.
 *
 * <p>
 * <table border="1">
 * <tr>
 * <th>Constant</th>
 * <th>Data Size</th>
 * <th>Power&nbsp;of&nbsp;2</th>
 * <th>Size in Bytes</th>
 * </tr>
 * <tr>
 * <td>{@link #BYTES}</td>
 * <td>1B</td>
 * <td>2^0</td>
 * <td>1</td>
 * </tr>
 * <tr>
 * <td>{@link #KILOBYTES}</td>
 * <td>1KB</td>
 * <td>2^10</td>
 * <td>1,024</td>
 * </tr>
 * <tr>
 * <td>{@link #MEGABYTES}</td>
 * <td>1MB</td>
 * <td>2^20</td>
 * <td>1,048,576</td>
 * </tr>
 * <tr>
 * <td>{@link #GIGABYTES}</td>
 * <td>1GB</td>
 * <td>2^30</td>
 * <td>1,073,741,824</td>
 * </tr>
 * <tr>
 * <td>{@link #TERABYTES}</td>
 * <td>1TB</td>
 * <td>2^40</td>
 * <td>1,099,511,627,776</td>
 * </tr>
 * </table>
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 * @since 2.3.0
 * @see Period
 */
public enum PeriodStyle {

	/**
	 * Simple formatting, for example '1s'.
	 */
	SIMPLE("^([\\+\\-]?\\d+)([a-zA-Z]{0,2})$") {

		@Override
		public Period parse(String value, ChronoUnit unit) {
			try {
				Matcher matcher = matcher(value);
				Assert.state(matcher.matches(), "Does not match simple period pattern");
				String suffix = matcher.group(2);
				return (StringUtils.hasLength(suffix) ? Unit.fromSuffix(suffix) : Unit.fromChronoUnit(unit))
						.parse(matcher.group(1));
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("'" + value + "' is not a valid simple period", ex);
			}
		}

		@Override
		public String print(Period value, ChronoUnit unit) {
			return Unit.fromChronoUnit(unit).print(value);
		}

	},

	/**
	 * ISO-8601 formatting.
	 */
	ISO8601("^[\\+\\-]?P.*$") {

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

	private final Pattern pattern;

	PeriodStyle(String pattern) {
		this.pattern = Pattern.compile(pattern);
	}

	protected final boolean matches(String value) {
		return this.pattern.matcher(value).matches();
	}

	protected final Matcher matcher(String value) {
		return this.pattern.matcher(value);
	}

	/**
	 * Parse the given value to a duration.
	 * @param value the value to parse
	 * @return a duration
	 */
	public Period parse(String value) {
		return parse(value, null);
	}

	/**
	 * Parse the given value to a duration.
	 * @param value the value to parse
	 * @param unit the duration unit to use if the value doesn't specify one ({@code null}
	 * will default to ms)
	 * @return a duration
	 */
	public abstract Period parse(String value, ChronoUnit unit);

	/**
	 * Print the specified duration.
	 * @param value the value to print
	 * @return the printed result
	 */
	public String print(Period value) {
		return print(value, null);
	}

	/**
	 * Print the specified duration using the given unit.
	 * @param value the value to print
	 * @param unit the value to use for printing
	 * @return the printed result
	 */
	public abstract String print(Period value, ChronoUnit unit);

	/**
	 * Detect the style then parse the value to return a duration.
	 * @param value the value to parse
	 * @return the parsed duration
	 * @throws IllegalStateException if the value is not a known style or cannot be parsed
	 */
	public static Period detectAndParse(String value) {
		return detectAndParse(value, null);
	}

	/**
	 * Detect the style then parse the value to return a duration.
	 * @param value the value to parse
	 * @param unit the duration unit to use if the value doesn't specify one ({@code null}
	 * will default to ms)
	 * @return the parsed duration
	 * @throws IllegalStateException if the value is not a known style or cannot be parsed
	 */
	public static Period detectAndParse(String value, ChronoUnit unit) {
		return detect(value).parse(value, unit);
	}

	/**
	 * Detect the style from the given source value.
	 * @param value the source value
	 * @return the duration style
	 * @throws IllegalStateException if the value is not a known style
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
		 * Days, represented by suffix {@code B}.
		 */
		DAYS(ChronoUnit.DAYS, "d", Period::getDays),

		/**
		 * Days, represented by suffix {@code B}.
		 */
		// WEEKS(ChronoUnit.WEEKS,"w", Period::ofWeeks),

		/**
		 * Days, represented by suffix {@code B}.
		 */
		MONTHS(ChronoUnit.MONTHS, "m", Period::getMonths),

		/**
		 * Days, represented by suffix {@code B}.
		 */
		YEARS(ChronoUnit.YEARS, "y", Period::getYears);

		private final ChronoUnit chronoUnit;

		private final String suffix;

		private final Function<Period, Integer> intValue;

		Unit(ChronoUnit chronoUnit, String suffix, Function<Period, Integer> intValue) {
			this.chronoUnit = chronoUnit;
			this.suffix = suffix;
			this.intValue = intValue;
		}

		/**
		 * Return the {@link Unit} matching the specified {@code suffix}.
		 * @param suffix one of the standard suffixes
		 * @return the {@link Unit} matching the specified {@code suffix}
		 * @throws IllegalArgumentException if the suffix does not match the suffix of any
		 * of this enum's constants
		 */
		public static Unit fromSuffix(String suffix) {
			for (Unit candidate : values()) {
				if (candidate.suffix.equalsIgnoreCase(suffix)) {
					return candidate;
				}
			}
			throw new IllegalArgumentException("Unknown unit suffix '" + suffix + "'");
		}

		public Period parse(String value) {

			int intValue = Integer.parseInt(value);

			if (ChronoUnit.DAYS == this.chronoUnit) {
				return Period.ofDays(intValue);
			}
			else if (ChronoUnit.WEEKS == this.chronoUnit) {
				return Period.ofWeeks(intValue);
			}
			else if (ChronoUnit.MONTHS == this.chronoUnit) {
				return Period.ofMonths(intValue);
			}
			else if (ChronoUnit.YEARS == this.chronoUnit) {
				return Period.ofYears(intValue);
			}

			throw new IllegalArgumentException("Unknow unit '" + this.chronoUnit + "'");
		}

		public String print(Period value) {
			return longValue(value) + this.suffix;
		}

		public long longValue(Period value) {
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
			throw new IllegalArgumentException("Unknown unit " + chronoUnit);
		}

	}

}
