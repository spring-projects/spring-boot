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

package org.springframework.boot.env;

import java.util.HexFormat;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
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
 * <li>When {@literal "uuid"}, a random {@link UUID} value.</li>
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
 * @author Moritz Halbritter
 * @since 1.0.0
 */
public class RandomValuePropertySource extends PropertySource<Random> {

	/**
	 * Name of the random {@link PropertySource}.
	 */
	public static final String RANDOM_PROPERTY_SOURCE_NAME = "random";

	private static final String PREFIX = "random.";

	private static final Log logger = LogFactory.getLog(RandomValuePropertySource.class);

	/**
     * Constructs a new RandomValuePropertySource with the default name.
     */
    public RandomValuePropertySource() {
		this(RANDOM_PROPERTY_SOURCE_NAME);
	}

	/**
     * Constructs a new RandomValuePropertySource with the specified name and a new instance of Random as the random number generator.
     * 
     * @param name the name of the property source
     */
    public RandomValuePropertySource(String name) {
		super(name, new Random());
	}

	/**
     * Retrieves the value of a property with the given name.
     * 
     * @param name the name of the property to retrieve
     * @return the value of the property, or null if the property does not exist
     */
    @Override
	public Object getProperty(String name) {
		if (!name.startsWith(PREFIX)) {
			return null;
		}
		logger.trace(LogMessage.format("Generating random property for '%s'", name));
		return getRandomValue(name.substring(PREFIX.length()));
	}

	/**
     * Generates a random value based on the given type.
     * 
     * @param type the type of value to generate (int, long, uuid, or any other custom type)
     * @return a random value of the specified type
     */
    private Object getRandomValue(String type) {
		if (type.equals("int")) {
			return getSource().nextInt();
		}
		if (type.equals("long")) {
			return getSource().nextLong();
		}
		String range = getRange(type, "int");
		if (range != null) {
			return getNextIntInRange(Range.of(range, Integer::parseInt));
		}
		range = getRange(type, "long");
		if (range != null) {
			return getNextLongInRange(Range.of(range, Long::parseLong));
		}
		if (type.equals("uuid")) {
			return UUID.randomUUID().toString();
		}
		return getRandomBytes();
	}

	/**
     * Returns the range of a given type based on a specified prefix.
     * 
     * @param type   the type to check for the prefix
     * @param prefix the prefix to match with the type
     * @return the range of the type if it starts with the prefix, null otherwise
     */
    private String getRange(String type, String prefix) {
		if (type.startsWith(prefix)) {
			int startIndex = prefix.length() + 1;
			if (type.length() > startIndex) {
				return type.substring(startIndex, type.length() - 1);
			}
		}
		return null;
	}

	/**
     * Generates a random integer within the specified range.
     * 
     * @param range the range within which the random integer should be generated
     * @return the generated random integer
     * @throws IllegalArgumentException if the range is invalid
     */
    private int getNextIntInRange(Range<Integer> range) {
		OptionalInt first = getSource().ints(1, range.getMin(), range.getMax()).findFirst();
		assertPresent(first.isPresent(), range);
		return first.getAsInt();
	}

	/**
     * Generates a random long value within the specified range.
     * 
     * @param range the range within which the random long value should be generated
     * @return a random long value within the specified range
     * @throws IllegalArgumentException if the range is invalid (i.e., min > max)
     * @throws NoSuchElementException if no value is found within the range
     */
    private long getNextLongInRange(Range<Long> range) {
		OptionalLong first = getSource().longs(1, range.getMin(), range.getMax()).findFirst();
		assertPresent(first.isPresent(), range);
		return first.getAsLong();
	}

	/**
     * Asserts that a random number is present for the given range.
     * 
     * @param present a boolean indicating if a random number is present
     * @param range the range for which the random number is being checked
     * @throws IllegalStateException if the random number is not present
     */
    private void assertPresent(boolean present, Range<?> range) {
		Assert.state(present, () -> "Could not get random number for range '" + range + "'");
	}

	/**
     * Generates a random byte array of length 16.
     * 
     * @return the randomly generated byte array in hexadecimal format
     */
    private Object getRandomBytes() {
		byte[] bytes = new byte[16];
		getSource().nextBytes(bytes);
		return HexFormat.of().withLowerCase().formatHex(bytes);
	}

	/**
     * Adds the RandomValuePropertySource to the given ConfigurableEnvironment.
     * 
     * @param environment the ConfigurableEnvironment to add the RandomValuePropertySource to
     */
    public static void addToEnvironment(ConfigurableEnvironment environment) {
		addToEnvironment(environment, logger);
	}

	/**
     * Adds the RandomValuePropertySource to the given ConfigurableEnvironment.
     * 
     * @param environment the ConfigurableEnvironment to add the RandomValuePropertySource to
     * @param logger the Log object used for logging
     */
    static void addToEnvironment(ConfigurableEnvironment environment, Log logger) {
		MutablePropertySources sources = environment.getPropertySources();
		PropertySource<?> existing = sources.get(RANDOM_PROPERTY_SOURCE_NAME);
		if (existing != null) {
			logger.trace("RandomValuePropertySource already present");
			return;
		}
		RandomValuePropertySource randomSource = new RandomValuePropertySource(RANDOM_PROPERTY_SOURCE_NAME);
		if (sources.get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME) != null) {
			sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, randomSource);
		}
		else {
			sources.addLast(randomSource);
		}
		logger.trace("RandomValuePropertySource add to Environment");
	}

	/**
     * Range class.
     */
    static final class Range<T extends Number> {

		private final String value;

		private final T min;

		private final T max;

		/**
         * Constructs a new Range object with the specified value, minimum value, and maximum value.
         * 
         * @param value the value of the range
         * @param min the minimum value of the range
         * @param max the maximum value of the range
         */
        private Range(String value, T min, T max) {
			this.value = value;
			this.min = min;
			this.max = max;
		}

		/**
         * Returns the minimum value of the range.
         *
         * @return the minimum value of the range
         */
        T getMin() {
			return this.min;
		}

		/**
         * Returns the maximum value of the range.
         *
         * @return the maximum value of the range
         */
        T getMax() {
			return this.max;
		}

		/**
         * Returns a string representation of the value of this Range object.
         *
         * @return the string representation of the value of this Range object
         */
        @Override
		public String toString() {
			return this.value;
		}

		/**
         * Creates a new Range object with the specified value and bounds.
         * 
         * @param value the string representation of the range
         * @param parse the function used to parse the string representation into a number
         * @return a new Range object with the specified value and bounds
         * @throws IllegalArgumentException if the bounds are invalid
         */
        static <T extends Number & Comparable<T>> Range<T> of(String value, Function<String, T> parse) {
			T zero = parse.apply("0");
			String[] tokens = StringUtils.commaDelimitedListToStringArray(value);
			T min = parse.apply(tokens[0]);
			if (tokens.length == 1) {
				Assert.isTrue(min.compareTo(zero) > 0, "Bound must be positive.");
				return new Range<>(value, zero, min);
			}
			T max = parse.apply(tokens[1]);
			Assert.isTrue(min.compareTo(max) < 0, "Lower bound must be less than upper bound.");
			return new Range<>(value, min, max);
		}

	}

}
