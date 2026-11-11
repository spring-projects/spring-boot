/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.logging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;

/**
 * Configures the classpath to prefer Log4j2. Other logging systems may still be on the
 * classpath, but Log4j2 will be positioned before them.
 *
 * @author Andy Wilkinson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@ClassPathExclusions("log4j-to-slf4j-*.jar")
@ClassPathOverrides({ "org.apache.logging.log4j:log4j-core:2.26.1",
		"org.apache.logging.log4j:log4j-slf4j-impl:2.26.1" })
public @interface ConfigureClasspathToPreferLog4j2 {

}
