/*
 * Copyright 2012-2019 the original author or authors.
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
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpringBootDependencyInjectionTestExecutionListener}.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class SpringBootDependencyInjectionTestExecutionListenerTests {

	private SpringBootDependencyInjectionTestExecutionListener reportListener = new SpringBootDependencyInjectionTestExecutionListener();

	@Test
	void orderShouldBeSameAsDependencyInjectionTestExecutionListener() {
		Ordered injectionListener = new DependencyInjectionTestExecutionListener();
		assertThat(this.reportListener.getOrder()).isEqualTo(injectionListener.getOrder());
	}

	@Test
	void prepareFailingTestInstanceShouldPrintReport(CapturedOutput output) throws Exception {
		TestContext testContext = mock(TestContext.class);
		given(testContext.getTestInstance()).willThrow(new IllegalStateException());
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext applicationContext = application.run();
		given(testContext.getApplicationContext()).willReturn(applicationContext);
		try {
			this.reportListener.prepareTestInstance(testContext);
		}
		catch (IllegalStateException ex) {
			// Expected
		}
		assertThat(output).contains("CONDITIONS EVALUATION REPORT").contains("Positive matches")
				.contains("Negative matches");
	}

	@Test
	void originalFailureIsThrownWhenReportGenerationFails() throws Exception {
		TestContext testContext = mock(TestContext.class);
		IllegalStateException originalFailure = new IllegalStateException();
		given(testContext.getTestInstance()).willThrow(originalFailure);
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		given(testContext.getApplicationContext()).willThrow(new RuntimeException());
		assertThatIllegalStateException().isThrownBy(() -> this.reportListener.prepareTestInstance(testContext))
				.isEqualTo(originalFailure);
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(JacksonAutoConfiguration.class)
	static class Config {

	}

}
