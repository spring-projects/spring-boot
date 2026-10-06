/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.context;

import java.util.logging.Logger;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoggingApplicationListener} when the test context framework pauses a
 * context while it's in the cache.
 *
 * @author Raphael Vullriede
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@TestClassOrder(ClassOrderer.ClassName.class)
class LoggingApplicationListenerTcfCacheContextPausingTests {

	private static void assertJulOutputIsLogged(CapturedOutput output) {
		Logger.getLogger("test").severe("Hello from JUL");
		assertThat(output).contains("Hello from JUL");
	}

	@Nested
	@Import(TestConfig.class)
	@TestPropertySource(properties = "context=one")
	class ContextOne {

		@Test
		void test(CapturedOutput output) {
			assertJulOutputIsLogged(output);
		}

	}

	@Nested
	@Import(TestConfig.class)
	@TestPropertySource(properties = "context=two")
	class ContextTwo {

		@Test
		void test(CapturedOutput output) {
			assertJulOutputIsLogged(output);
		}

	}

	@Nested
	@Import(TestConfig.class)
	@TestPropertySource(properties = "context=one")
	class ReuseContextOne {

		@Test
		void test(CapturedOutput output) {
			assertJulOutputIsLogged(output);
		}

	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	static class TestConfig {

	}

}
