/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.test.autoconfigure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.cache.ContextCache;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link OnFailureConditionReportContextCustomizerFactory}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
class OnFailureConditionReportContextCustomizerFactoryTests {

	@BeforeEach
	void clearCache() {
		ContextCache contextCache = (ContextCache) ReflectionTestUtils
			.getField(DefaultCacheAwareContextLoaderDelegate.class, "defaultContextCache");
		if (contextCache != null) {
			contextCache.reset();
		}
	}

	@Test
	void loadFailureShouldPrintReport(CapturedOutput output) {
		load();
		assertThat(output.getErr()).contains("JacksonAutoConfiguration matched");
		assertThat(output).contains("Error creating bean with name 'faultyBean'");
	}

	@Test
	@WithResource(name = "application.xml", content = "invalid xml")
	void loadFailureShouldNotPrintReportWhenApplicationPropertiesIsBroken(CapturedOutput output) {
		load();
		assertThat(output).doesNotContain("JacksonAutoConfiguration matched")
			.doesNotContain("Error creating bean with name 'faultyBean'")
			.contains("java.util.InvalidPropertiesFormatException");
	}

	@Test
	@WithResource(name = "application.properties", content = "spring.test.print-condition-evaluation-report=false")
	void loadFailureShouldNotPrintReportWhenDisabled(CapturedOutput output) {
		load();
		assertThat(output).doesNotContain("JacksonAutoConfiguration matched")
			.contains("Error creating bean with name 'faultyBean'");
	}

	private void load() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new TestContextManager(FailingTests.class).getTestContext().getApplicationContext());
	}

	@SpringBootTest
	static class FailingTests {

		@Configuration(proxyBeanMethods = false)
		@ImportAutoConfiguration(JacksonAutoConfiguration.class)
		static class TestConfig {

			@Bean
			String faultyBean() {
				throw new IllegalStateException();
			}

		}

	}

}
