/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.env;

import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * {@link PropertySource} that returns a random value for any property that starts with
 * {@literal "random."}. Where the "unqualified property name" is the portion of the
 * requested property name beyond the "random." prefix, this {@link PropertySource}
 * returns:
 * <ul>
 * <li>When {@literal "int"}, a random {@link Integer} value, restricted by an optionally
 * specified range.</li>
 * <li>When {@literal "long"}, a random {@link Long} value, restricted by an optionally
 * specified range.</li>
 * <li>Otherwise, a {@code byte[]}.</li>
 * </ul>
 * The {@literal "random.int"} and {@literal "random.long"} properties supports a range
 * suffix whose syntax is:
 * <p>
 * {@code OPEN value (,max) CLOSE} where the {@code OPEN,CLOSE} are any character and
 * {@code value,max} are integers. If {@code max} is not provided, then 0 is used as the
 * lower bound and {@code value} is the upper bound. If {@code max} is provided then
 * {@code value} is the minimum value and {@code max} is the maximum (exclusive).
 *
 * @author Dave Syer
 * @author Matt Benson
 * @author Madhura Bhave
 * @since 1.0.0
 */
public class RandomValuePropertySource extends PropertySource<Random> {

	/**
	 * Name of the random {@link PropertySource}.
	 */
	public static final String RANDOM_PROPERTY_SOURCE_NAME = "random";

	private static final String PREFIX = "random.";

	private static final Log logger = LogFactory.getLog(RandomValuePropertySource.class);

	public RandomValuePropertySource() {
		this(RANDOM_PROPERTY_SOURCE_NAME);
	}

	public RandomValuePropertySource(String name) {
		super(name, new Random());
	}

	@Override
	public Object getProperty(String name) {
		if (!name.startsWith(PREFIX)) {
			return null;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Generating random property for '" + name + "'");
		}
		return getRandomValue(name.substring(PREFIX.length()));
	}

	private Object getRandomValue(String type) {
		if (type.equals("int")) {
			return getSource().nextInt();
		}
		if (type.equals("long")) {
			return getSource().nextLong();
		}
		String range = getRange(type, "int");
		if (range != null) {
			return getNextIntInRange(range);
		}
		range = getRange(type, "long");
		if (range != null) {
			return getNextLongInRange(range);
		}
		if (type.equals("uuid")) {
			return UUID.randomUUID().toString();
		}
		return getRandomBytes();
	}

	private String getRange(String type, String prefix) {
		if (type.startsWith(prefix)) {
			int startIndex = prefix.length() + 1;
			if (type.length() > startIndex) {
				return type.substring(startIndex, type.length() - 1);
			}
		}
		return null;
	}

	private int getNextIntInRange(String range) {
		Range<Integer> intRange = Range.get(range, Integer::parseInt, (t) -> t > 0, 0, (t1, t2) -> t1 < t2);
		OptionalInt first = getSource().ints(1, intRange.getMin(), intRange.getMax()).findFirst();
		if (!first.isPresent()) {
			throw new RuntimeException("Could not get random number for range '" + range + "'");
		}
		return first.getAsInt();
	}

	private long getNextLongInRange(String range) {
		Range<Long> longRange = Range.get(range, Long::parseLong, (t) -> t > 0L, 0L, (t1, t2) -> t1 < t2);
		OptionalLong first = getSource().longs(1, longRange.getMin(), longRange.getMax()).findFirst();
		if (!first.isPresent()) {
			throw new RuntimeException("Could not get random number for range '" + range + "'");
		}
		return first.getAsLong();
	}

	private Object getRandomBytes() {
		byte[] bytes = new byte[32];
		getSource().nextBytes(bytes);
		return DigestUtils.md5DigestAsHex(bytes);
	}

	public static void addToEnvironment(ConfigurableEnvironment environment) {
		environment.getPropertySources().addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				new RandomValuePropertySource(RANDOM_PROPERTY_SOURCE_NAME));
		logger.trace("RandomValuePropertySource add to Environment");
	}

	static final class Range<T extends Number> {

		private final T min;

		private final T max;

		private Range(T min, T max) {
			this.min = min;
			this.max = max;

		}

		static <T extends Number> Range<T> get(String range, Function<String, T> parse, Predicate<T> boundValidator,
				T defaultMin, BiPredicate<T, T> rangeValidator) {
			String[] tokens = StringUtils.commaDelimitedListToStringArray(range);
			T token1 = parse.apply(tokens[0]);
			if (tokens.length == 1) {
				Assert.isTrue(boundValidator.test(token1), "Bound must be positive.");
				return new Range<>(defaultMin, token1);
			}
			T token2 = parse.apply(tokens[1]);
			Assert.isTrue(rangeValidator.test(token1, token2), "Lower bound must be less than upper bound.");
			return new Range<>(token1, token2);
		}

		T getMin() {
			return this.min;
		}

		T getMax() {
			return this.max;
		}

	}

}
