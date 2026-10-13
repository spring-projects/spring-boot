/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

/**
 * Converts from a String to a {@link java.lang.Enum} with lenient conversion rules.
 * Specifically:
 * <ul>
 * <li>Uses a case insensitive search</li>
 * <li>Does not consider {@code '_'}, {@code '$'} or other special characters</li>
 * <li>Allows mapping of {@code "false"} and {@code "true"} to enums {@code ON} and
 * {@code OFF}</li>
 * </ul>
 *
 * @author Phillip Webb
 */
final class LenientStringToEnumConverterFactory extends LenientObjectToEnumConverterFactory<String> {

}
