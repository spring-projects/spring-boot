/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.process;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Disables test execution if a process is unavailable.
 *
 * @author Phillip Webb
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(DisabledIfProcessUnavailableCondition.class)
@Repeatable(DisabledIfProcessUnavailables.class)
public @interface DisabledIfProcessUnavailable {

	String[] value();

}
