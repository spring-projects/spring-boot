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

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.webdriver.MockMvcHtmlUnitDriverBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} with {@link WebDriver} in a custom scope.
 *
 * @author Phillip Webb
 */
@WebMvcTest
@TestMethodOrder(MethodOrderer.MethodName.class)
class WebMvcTestWebDriverCustomScopeIntegrationTests {

	// gh-7454

	private static WebDriver previousWebDriver;

	@Autowired
	private WebDriver webDriver;

	@Test
	void shouldAutoConfigureWebClient() {
		WebMvcTestWebDriverCustomScopeIntegrationTests.previousWebDriver = this.webDriver;
	}

	@Test
	void shouldBeTheSameWebClient() {
		assertThat(previousWebDriver).isNotNull().isSameAs(this.webDriver);
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
		WebDriverFactory webDriver(MockMvc mockMvc) {
			return new WebDriverFactory(mockMvc);
		}

	}

	static class WebDriverFactory implements FactoryBean<WebDriver> {

		private final HtmlUnitDriver driver;

		WebDriverFactory(MockMvc mockMvc) {
			this.driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(mockMvc).build();
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public Class<?> getObjectType() {
			return WebDriver.class;
		}

		@Override
		public WebDriver getObject() {
			return this.driver;
		}

	}

}
