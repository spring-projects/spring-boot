/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.core.convert.ConversionService;

/**
 * Specialized {@link ParameterizedTest parameterized test} for
 * {@link ConversionService}-related testing. Test classes with methods annotated with
 * {@link ParameterizedTest @ParameterizedTest} must have a {@code static}
 * {@code conversionServices} method that is suitable for use as a {@link MethodSource
 * method source}.
 *
 * @author Andy Wilkinson
 */
@ParameterizedTest
@MethodSource("conversionServices")
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConversionServiceTest {

}
