/*
 * Copyright 2012-2023 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionReportApplicationContextFailureProcessor}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
@ExtendWith(OutputCaptureExtension.class)
class ConditionReportApplicationContextFailureProcessorTests {

	@Test
	void loadFailureShouldPrintReport(CapturedOutput output) throws Exception {
		SpringApplication application = new SpringApplication(TestConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext applicationContext = application.run();
		ConditionReportApplicationContextFailureProcessor processor = new ConditionReportApplicationContextFailureProcessor();
		processor.processLoadFailure(applicationContext, new IllegalStateException());
		assertThat(output).contains("CONDITIONS EVALUATION REPORT")
			.contains("Positive matches")
			.contains("Negative matches");
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(JacksonAutoConfiguration.class)
	static class TestConfig {

	}

}
