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
 * Alternative to Spring Boot's {@code @RestControllerEndpoint} for testing (removes the
 * need for a dependency on the real annotation).
 *
 * @author Andy Wilkinson
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TestRestControllerEndpoint {

	String id() default "";

	TestAccess defaultAccess() default TestAccess.UNRESTRICTED;

}
