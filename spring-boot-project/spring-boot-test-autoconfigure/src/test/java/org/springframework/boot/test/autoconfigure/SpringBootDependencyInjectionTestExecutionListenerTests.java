/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpringBootDependencyInjectionTestExecutionListener}.
 *
 * @author Phillip Webb
 */
public class SpringBootDependencyInjectionTestExecutionListenerTests {

	@Rule
	public OutputCapture out = new OutputCapture();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private SpringBootDependencyInjectionTestExecutionListener reportListener = new SpringBootDependencyInjectionTestExecutionListener();

	@Test
	public void orderShouldBeSameAsDependencyInjectionTestExecutionListener() {
		Ordered injectionListener = new DependencyInjectionTestExecutionListener();
		assertThat(this.reportListener.getOrder())
				.isEqualTo(injectionListener.getOrder());
	}

	@Test
	public void prepareFailingTestInstanceShouldPrintReport() throws Exception {
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
		this.out.expect(containsString("CONDITIONS EVALUATION REPORT"));
		this.out.expect(containsString("Positive matches"));
		this.out.expect(containsString("Negative matches"));
	}

	@Test
	public void originalFailureIsThrownWhenReportGenerationFails() throws Exception {
		TestContext testContext = mock(TestContext.class);
		IllegalStateException originalFailure = new IllegalStateException();
		given(testContext.getTestInstance()).willThrow(originalFailure);
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		given(testContext.getApplicationContext()).willThrow(new RuntimeException());
		this.thrown.expect(is(originalFailure));
		this.reportListener.prepareTestInstance(testContext);
	}

	@Configuration
	@ImportAutoConfiguration(JacksonAutoConfiguration.class)
	static class Config {

	}

}
