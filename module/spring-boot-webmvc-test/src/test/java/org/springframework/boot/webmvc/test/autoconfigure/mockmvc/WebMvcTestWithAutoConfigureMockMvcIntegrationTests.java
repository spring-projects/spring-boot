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

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.htmlunit.WebClient;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} with
 * {@link AutoConfigureMockMvc @AutoConfigureMockMvc}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false, webClientEnabled = false, webDriverEnabled = false)
class WebMvcTestWithAutoConfigureMockMvcIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MockMvcTester mvc;

	@Test
	void shouldNotAddFilters() {
		assertThat(this.mvc.get().uri("/one")).doesNotContainHeader("x-test");
	}

	@Test
	void shouldNotHaveWebDriver() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.context.getBean(WebDriver.class));
	}

	@Test
	void shouldNotHaveWebClient() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.context.getBean(WebClient.class));
	}

}
