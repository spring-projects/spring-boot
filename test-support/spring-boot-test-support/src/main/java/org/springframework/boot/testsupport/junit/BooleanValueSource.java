/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * {@code @BooleanValueSource} is an {@link ArgumentsSource} which provides {@code true}
 * and {@code false} values to the annotated {@code @ParameterizedTest} method.
 *
 * @author Scott Frederick
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(BooleanArgumentsProvider.class)
public @interface BooleanValueSource {

}
