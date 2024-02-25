/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.type;

import java.util.Random;
import java.util.stream.IntStream;

import org.springframework.util.Assert;

/**
 * Utility class used to generate random strings.
 *
 * @author Phillip Webb
 */
final class RandomString {

	private static final Random random = new Random();

	/**
     * This is a private constructor for the RandomString class.
     * It is used to create an instance of the RandomString class.
     * 
     * Note: This constructor is private and cannot be accessed from outside the class.
     */
    private RandomString() {
	}

	/**
     * Generates a random string with a given prefix and length.
     *
     * @param prefix        the prefix to be added to the generated string (must not be null)
     * @param randomLength  the length of the random string to be generated
     * @return              the generated string with the prefix
     * @throws IllegalArgumentException if the prefix is null
     */
    static String generate(String prefix, int randomLength) {
		Assert.notNull(prefix, "Prefix must not be null");
		return prefix + generateRandom(randomLength);
	}

	/**
     * Generates a random string of specified length.
     *
     * @param length the length of the random string to be generated
     * @return a CharSequence containing the randomly generated string
     */
    static CharSequence generateRandom(int length) {
		IntStream chars = random.ints('a', 'z' + 1).limit(length);
		return chars.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append);
	}

}
