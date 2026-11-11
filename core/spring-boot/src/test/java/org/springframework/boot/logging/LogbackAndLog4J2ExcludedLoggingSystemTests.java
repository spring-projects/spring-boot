/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging;

import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.java.JavaLoggingSystem;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoggingSystem} when Logback is not on the classpath.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions({ "log4j-*.jar", "logback-*.jar" })
class LogbackAndLog4J2ExcludedLoggingSystemTests {

	@Test
	void whenLogbackAndLog4J2AreNotPresentJULIsTheLoggingSystem() {
		assertThat(LoggingSystem.get(getClass().getClassLoader())).isInstanceOf(JavaLoggingSystem.class);
	}

}
