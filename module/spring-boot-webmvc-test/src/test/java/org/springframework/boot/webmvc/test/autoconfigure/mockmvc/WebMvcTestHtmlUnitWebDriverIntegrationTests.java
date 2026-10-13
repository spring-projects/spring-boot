/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc.HtmlUnit;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} with {@link WebDriver}.
 *
 * @author Phillip Webb
 */
@WebMvcTest
@TestMethodOrder(MethodOrderer.MethodName.class)
@AutoConfigureMockMvc(htmlUnit = @HtmlUnit(url = "http://localhost:8181"))
class WebMvcTestHtmlUnitWebDriverIntegrationTests {

	private static @Nullable WebDriver previousWebDriver;

	@Autowired
	private WebDriver webDriver;

	@Test
	void shouldAutoConfigureWebClient() {
		this.webDriver.get("/html");
		WebElement element = this.webDriver.findElement(By.tagName("body"));
		assertThat(element.getText()).isEqualTo("Hello");
		WebMvcTestHtmlUnitWebDriverIntegrationTests.previousWebDriver = this.webDriver;
	}

	@Test
	void shouldBeADifferentWebClient() {
		this.webDriver.get("/html");
		WebElement element = this.webDriver.findElement(By.tagName("body"));
		assertThat(element.getText()).isEqualTo("Hello");
		assertThat(previousWebDriver).isNotNull().isNotSameAs(this.webDriver);
		assertThatExceptionOfType(NoSuchSessionException.class).isThrownBy(previousWebDriver::getWindowHandle);
		assertThat(this.webDriver.getCurrentUrl()).isEqualTo("http://localhost:8181/html");
	}

}
