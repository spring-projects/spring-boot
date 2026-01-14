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

package org.springframework.boot.restclient.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockRestServiceServerAutoConfiguration}.
 *
 * @author HuitaePark
 */
class MockRestServiceServerAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MockRestServiceServerAutoConfiguration.class));

	@Test
	void registersRestClientCustomizerWhenMissing() {
		this.contextRunner.withPropertyValues("spring.test.restclient.mockrestserviceserver.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(MockServerRestClientCustomizer.class));
	}

	@Test
	void backsOffWhenUserProvidesRestClientCustomizer() {
		MockServerRestClientCustomizer customCustomizer = new MockServerRestClientCustomizer();

		this.contextRunner.withPropertyValues("spring.test.restclient.mockrestserviceserver.enabled=true")
			.withBean("userMockServerRestClientCustomizer", MockServerRestClientCustomizer.class,
					() -> customCustomizer)
			.run((context) -> {
				assertThat(context).hasSingleBean(MockServerRestClientCustomizer.class);
				assertThat(context.getBean(MockServerRestClientCustomizer.class))
					.isSameAs(context.getBean("userMockServerRestClientCustomizer"));
			});
	}

}
