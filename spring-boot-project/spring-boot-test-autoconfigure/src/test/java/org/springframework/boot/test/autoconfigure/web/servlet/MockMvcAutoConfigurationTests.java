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
package org.springframework.boot.test.autoconfigure.web.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockMvcAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
class MockMvcAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MockMvcAutoConfiguration.class));

	@Test
	void registersDispatcherServletFromMockMvc() {
		this.contextRunner.run((context) -> {
			MockMvc mockMvc = context.getBean(MockMvc.class);
			assertThat(context).hasSingleBean(DispatcherServlet.class);
			assertThat(context.getBean(DispatcherServlet.class)).isEqualTo(mockMvc.getDispatcherServlet());
		});
	}

}
