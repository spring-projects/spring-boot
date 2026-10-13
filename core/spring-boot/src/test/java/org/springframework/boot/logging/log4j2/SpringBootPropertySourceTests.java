/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootPropertySource}.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions({ "jakarta.servlet-api-*.jar", "tomcat-embed-core-*.jar" })
class SpringBootPropertySourceTests {

	@Test
	void propertySourceHasDisabledShutdownHook() {
		// Log4j2 disables the hook automatically in a web app so we check that it doesn't
		// think it's in one
		assertThat(Constants.IS_WEB_APP).isFalse();
		assertThat(((ShutdownCallbackRegistry) LogManager.getFactory()).addShutdownCallback(() -> {
		})).isNull();
	}

	@Test
	void allDefaultMethodsAreImplemented() {
		assertThat(Stream.of(SpringBootPropertySource.class.getMethods()).filter(Method::isDefault)).isEmpty();
	}

}
