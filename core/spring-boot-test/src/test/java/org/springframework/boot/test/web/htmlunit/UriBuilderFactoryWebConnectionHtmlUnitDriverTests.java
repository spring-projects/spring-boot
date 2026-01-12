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

import java.net.URL;

import org.htmlunit.TopLevelWindow;
import org.htmlunit.WebClient;
import org.htmlunit.WebClientOptions;
import org.htmlunit.WebConsole;
import org.htmlunit.WebRequest;
import org.htmlunit.WebWindow;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import org.springframework.test.web.servlet.htmlunit.webdriver.WebConnectionHtmlUnitDriver;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UriBuilderFactoryWebConnectionHtmlUnitDriver}.
 *
 * @author Phillip Webb
 */
class UriBuilderFactoryWebConnectionHtmlUnitDriverTests {

	private final WebClient webClient;

	UriBuilderFactoryWebConnectionHtmlUnitDriverTests() {
		this.webClient = mock();
		given(this.webClient.getOptions()).willReturn(new WebClientOptions());
		given(this.webClient.getWebConsole()).willReturn(new WebConsole());
		WebWindow currentWindow = mock(WebWindow.class);
		given(currentWindow.isClosed()).willReturn(false);
		given(this.webClient.getCurrentWindow()).willReturn(currentWindow);
	}

	@Test
	void getWhenUrlIsRelativeUsesBaseUrl() throws Exception {
		WebConnectionHtmlUnitDriver driver = new TestUriBuilderFactoryWebConnectionHtmlUnitDriver(
				new DefaultUriBuilderFactory("https://localhost:8080"));
		driver.get("/test");
		then(this.webClient).should()
			.getPage(any(TopLevelWindow.class), requestToUrl(new URL("https://localhost:8080/test")));
	}

	private WebRequest requestToUrl(URL url) {
		return argThat(new WebRequestUrlArgumentMatcher(url));
	}

	class TestUriBuilderFactoryWebConnectionHtmlUnitDriver extends UriBuilderFactoryWebConnectionHtmlUnitDriver {

		TestUriBuilderFactoryWebConnectionHtmlUnitDriver(UriBuilderFactory uriBuilderFactory) {
			super(uriBuilderFactory);
		}

		@Override
		public WebClient getWebClient() {
			return UriBuilderFactoryWebConnectionHtmlUnitDriverTests.this.webClient;
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
