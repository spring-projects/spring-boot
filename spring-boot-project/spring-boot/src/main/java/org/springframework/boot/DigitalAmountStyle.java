/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

/**
 * {@code DigitalAmount} format styles.
 *
 * @author Dmytro Nosan
 * @since 2.1.0
 */
public enum DigitalAmountStyle {

	/**
	 * Simple formatting, for example '1GB', '1MB', '1B', etc...
	 */
	SIMPLE("^\\s*([\\+\\-]?\\d+)\\s*([a-zA-Z]{0,2})\\s*$") {
		@Override
		public DigitalAmount parse(CharSequence value, DigitalUnit unit) {
			Assert.notNull(value, () -> "Digital Amount pattern must not be null");
			try {
				Matcher matcher = matcher(value);
				Assert.state(matcher.matches(),
						"Does not match simple Digital Amount " + "pattern");
				long amount = NumberUtils
						.parseNumber(matcher.group(1).toLowerCase(Locale.US), Long.class);
				String abbreviation = matcher.group(2);
				if (StringUtils.hasText(abbreviation)) {
					unit = DigitalUnit.fromAbbreviation(abbreviation);
				}
				return DigitalAmount.fromUnit(amount,
						Optional.ofNullable(unit).orElse(DigitalUnit.BYTES));
			}
			catch (Exception ex) {
				throw new IllegalArgumentException(
						"'" + value + "' is not a valid simple digital amount", ex);
			}
		}

		@Override
		public String print(DigitalAmount value, DigitalUnit unit) {
			Assert.notNull(value, () -> "Digital Amount must not be null");
			if (unit == null) {
				unit = DigitalUnit.BYTES;
			}
			long amount = unit.fromUnit(value.toBytes(), DigitalUnit.BYTES);
			return String.format("%d%s", amount, unit.getAbbreviation());
		}

	};

	private final Pattern pattern;

	DigitalAmountStyle(String pattern) {
		this.pattern = Pattern.compile(pattern);
	}

	protected final boolean matches(CharSequence value) {
		return this.pattern.matcher(value).matches();
	}

	protected final Matcher matcher(CharSequence value) {
		return this.pattern.matcher(value);
	}

	/**
	 * Parse the given value to a {@code DigitalAmount}.
	 * @param value the value to parse
	 * @return a {@code DigitalAmount}
	 */
	public final DigitalAmount parse(CharSequence value) {
		return parse(value, null);
	}

	/**
	 * Parse the given value to a {@code DigitalAmount}.
	 * @param value the value to parse
	 * @param unit {@code DigitalUnit} to use if one is not specified, default value is
	 * {@link DigitalUnit#BYTES}
	 * @return a {@code DigitalAmount}
	 */
	public abstract DigitalAmount parse(CharSequence value, DigitalUnit unit);

	/**
	 * Print the specified {@code DigitalAmount}.
	 * @param value the value to print
	 * @return the printed result
	 */
	public final String print(DigitalAmount value) {
		return print(value, null);
	}

	/**
	 * Print the specified {@code DigitalAmount} using the given unit.
	 * @param value the value to print
	 * @param unit the {@code DigitalUnit} to use, if {@code null} then
	 * {@link DigitalUnit#BYTES} will be used.
	 * @return the printed result
	 */
	public abstract String print(DigitalAmount value, DigitalUnit unit);

	/**
	 * Detect the style from the given source value.
	 * @param value the source value
	 * @return the digital amount style
	 * @throws IllegalStateException if the value is not a known style
	 */
	public static DigitalAmountStyle detect(CharSequence value) {
		Assert.notNull(value, "Digital Amount pattern must not be null");
		for (DigitalAmountStyle candidate : values()) {
			if (candidate.matches(value)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException(
				"'" + value + "' is not a valid digital " + "amount");
	}

}
