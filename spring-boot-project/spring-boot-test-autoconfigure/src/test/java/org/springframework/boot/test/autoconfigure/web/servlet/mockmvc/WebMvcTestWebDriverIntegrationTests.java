/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.web.servlet.mockmvc;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} with {@link WebDriver}.
 *
 * @author Phillip Webb
 */
@WebMvcTest
@WithMockUser
@TestMethodOrder(MethodOrderer.MethodName.class)
class WebMvcTestWebDriverIntegrationTests {

	private static WebDriver previousWebDriver;

	@Autowired
	private WebDriver webDriver;

	@Test
	void shouldAutoConfigureWebClient() {
		this.webDriver.get("/html");
		WebElement element = this.webDriver.findElement(By.tagName("body"));
		assertThat(element.getText()).isEqualTo("Hello");
		WebMvcTestWebDriverIntegrationTests.previousWebDriver = this.webDriver;
	}

	@Test
	void shouldBeADifferentWebClient() {
		this.webDriver.get("/html");
		WebElement element = this.webDriver.findElement(By.tagName("body"));
		assertThat(element.getText()).isEqualTo("Hello");
		assertThatExceptionOfType(NoSuchWindowException.class).isThrownBy(previousWebDriver::getWindowHandle);
		assertThat(previousWebDriver).isNotNull().isNotSameAs(this.webDriver);
	}

}
