/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample;

/**
 * Alternative to Spring Boot's {@code @Access} for testing (removes the need for a
 * dependency on the real annotation).
 *
 * @author Andy Wilkinson
 */
public enum TestAccess {

	NONE,

	READ_ONLY,

	UNRESTRICTED

}
