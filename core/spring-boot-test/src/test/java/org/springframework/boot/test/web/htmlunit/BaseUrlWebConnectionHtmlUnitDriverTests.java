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

package org.springframework.boot.test.web.htmlunit;

import java.net.MalformedURLException;
import java.net.URL;

import org.htmlunit.TopLevelWindow;
import org.htmlunit.WebClient;
import org.htmlunit.WebClientOptions;
import org.htmlunit.WebConsole;
import org.htmlunit.WebRequest;
import org.htmlunit.WebWindow;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.openqa.selenium.WebDriverException;

import org.springframework.boot.test.http.server.BaseUrl;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BaseUrlWebConnectionHtmlUnitDriver}.
 *
 * @author Phillip Webb
 */
class BaseUrlWebConnectionHtmlUnitDriverTests {

	private final WebClient webClient;

	BaseUrlWebConnectionHtmlUnitDriverTests() {
		this.webClient = mock();
		given(this.webClient.getOptions()).willReturn(new WebClientOptions());
		given(this.webClient.getWebConsole()).willReturn(new WebConsole());
		WebWindow currentWindow = mock(WebWindow.class);
		given(currentWindow.isClosed()).willReturn(false);
		given(this.webClient.getCurrentWindow()).willReturn(currentWindow);
	}

	@Test
	void createWhenBaseUrlIsNull() {
		BaseUrlWebConnectionHtmlUnitDriver driver = new TestBaseUrlWebConnectionHtmlUnitDriver(null);
		assertThatExceptionOfType(WebDriverException.class).isThrownBy(() -> driver.get("/test"))
			.withCauseInstanceOf(MalformedURLException.class);
	}

	@Test
	void getWhenUrlIsRelativeUsesBaseUrl() throws Exception {
		BaseUrl baseUrl = BaseUrl.of("https://example.com");
		BaseUrlWebConnectionHtmlUnitDriver driver = new TestBaseUrlWebConnectionHtmlUnitDriver(baseUrl);
		driver.get("/test");
		then(this.webClient).should()
			.getPage(any(TopLevelWindow.class), requestToUrl(new URL("https://example.com/test")));
	}

	private WebRequest requestToUrl(URL url) {
		return argThat(new WebRequestUrlArgumentMatcher(url));
	}

	public class TestBaseUrlWebConnectionHtmlUnitDriver extends BaseUrlWebConnectionHtmlUnitDriver {

		TestBaseUrlWebConnectionHtmlUnitDriver(@Nullable BaseUrl baseUrl) {
			super(baseUrl);
		}

		@Override
		public WebClient getWebClient() {
			return BaseUrlWebConnectionHtmlUnitDriverTests.this.webClient;
		}

	}

	private static final class WebRequestUrlArgumentMatcher implements ArgumentMatcher<WebRequest> {

		private final URL expectedUrl;

		private WebRequestUrlArgumentMatcher(URL expectedUrl) {
			this.expectedUrl = expectedUrl;
		}

		@Override
		public boolean matches(WebRequest argument) {
			return argument.getUrl().equals(this.expectedUrl);
		}

	}

}
