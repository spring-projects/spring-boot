/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import java.util.logging.Logger;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.test.context.LoggingApplicationListenerWithContextPausingTests.TestConfig;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoggingApplicationListener} when the test context framework pauses a
 * context while it's in the cache.
 *
 * @author Raphael Vullriede
 */
@SpringBootTest(classes = TestConfig.class)
@ExtendWith(OutputCaptureExtension.class)
@TestClassOrder(ClassOrderer.ClassName.class)
class LoggingApplicationListenerWithContextPausingTests {

	private static void assertJulOutputIsLogged(CapturedOutput output) {
		Logger.getLogger("test").severe("Hello from JUL");
		assertThat(output).contains("Hello from JUL");
	}

	@Nested
	@TestPropertySource(properties = "context=one")
	class ContextOne {

		@Test
		void test(CapturedOutput output) {
			assertJulOutputIsLogged(output);
		}

	}

	@Nested
	@TestPropertySource(properties = "context=two")
	class ContextTwo {

		@Test
		void test(CapturedOutput output) {
			assertJulOutputIsLogged(output);
		}

	}

	@Nested
	@TestPropertySource(properties = "context=one")
	class ReuseContextOne {

		@Test
		void test(CapturedOutput output) {
			assertJulOutputIsLogged(output);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfig {

	}

}
