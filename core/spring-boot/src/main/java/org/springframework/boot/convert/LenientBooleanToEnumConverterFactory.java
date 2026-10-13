/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

/**
 * Converter to support mapping of YAML style {@code "false"} and {@code "true"} to enums
 * {@code ON} and {@code OFF}.
 *
 * @author Madhura Bhave
 */
final class LenientBooleanToEnumConverterFactory extends LenientObjectToEnumConverterFactory<Boolean> {

}
