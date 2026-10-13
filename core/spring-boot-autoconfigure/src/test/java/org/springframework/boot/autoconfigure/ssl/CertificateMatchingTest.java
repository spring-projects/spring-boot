/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.ssl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Annotation for a {@code ParameterizedTest @ParameterizedTest} with a
 * {@link CertificateMatchingTestSource} parameter.
 *
 * @author Phillip Webb
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ParameterizedTest(name = "{0}")
@MethodSource("org.springframework.boot.autoconfigure.ssl.CertificateMatchingTestSource#create")
public @interface CertificateMatchingTest {

}
