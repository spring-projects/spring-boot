/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.container;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Disables test execution if Docker is unavailable.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(DisabledIfDockerUnavailableCondition.class)
public @interface DisabledIfDockerUnavailable {

}
