/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.process;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Repeatable container for {@link DisabledIfProcessUnavailable}.
 *
 * @author Phillip Webb
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(DisabledIfProcessUnavailableCondition.class)
public @interface DisabledIfProcessUnavailables {

	DisabledIfProcessUnavailable[] value();

}
