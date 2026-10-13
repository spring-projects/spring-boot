/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc.HtmlUnit;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} with {@link WebClient}.
 *
 * @author Phillip Webb
 */
@WebMvcTest
@AutoConfigureMockMvc(htmlUnit = @HtmlUnit(url = "http://localhost:8181"))
class WebMvcTestHtmlUnitWebClientIntegrationTests {

	@Autowired
	private WebClient webClient;

	@Test
	void shouldAutoConfigureWebClient() throws Exception {
		HtmlPage page = this.webClient.getPage("/html");
		assertThat(page.getBody().getTextContent()).isEqualTo("Hello");
		assertThat(page.getBaseURI()).isEqualTo("http://localhost:8181/html");
	}

}
