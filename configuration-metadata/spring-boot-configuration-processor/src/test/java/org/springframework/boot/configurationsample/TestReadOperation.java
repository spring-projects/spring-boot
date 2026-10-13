/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Alternative to Spring Boot's {@code @ReadOperation} for testing (removes the need for a
 * dependency on the real annotation).
 *
 * @author Stephane Nicoll
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TestReadOperation {

	String[] produces() default {};

}
