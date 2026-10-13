/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.htmlunit.WebClient;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc.HtmlUnit;
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
@AutoConfigureMockMvc(addFilters = false, htmlUnit = @HtmlUnit(webClient = false, webDriver = false))
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
