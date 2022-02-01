/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.web.htmlunit.webdriver;

import java.net.URL;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.WebConsole;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebWindow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.Capabilities;

import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LocalHostWebConnectionHtmlUnitDriver}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class LocalHostWebConnectionHtmlUnitDriverTests {

	private final WebClient webClient;

	LocalHostWebConnectionHtmlUnitDriverTests(@Mock WebClient webClient) {
		this.webClient = webClient;
		given(this.webClient.getOptions()).willReturn(new WebClientOptions());
		given(this.webClient.getWebConsole()).willReturn(new WebConsole());
	}

	@Test
	void createWhenEnvironmentIsNullWillThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new LocalHostWebConnectionHtmlUnitDriver(null))
				.withMessageContaining("Environment must not be null");
	}

	@Test
	void createWithJavascriptFlagWhenEnvironmentIsNullWillThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new LocalHostWebConnectionHtmlUnitDriver(null, true))
				.withMessageContaining("Environment must not be null");
	}

	@Test
	void createWithBrowserVersionWhenEnvironmentIsNullWillThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new LocalHostWebConnectionHtmlUnitDriver(null, BrowserVersion.CHROME))
				.withMessageContaining("Environment must not be null");
	}

	@Test
	void createWithCapabilitiesWhenEnvironmentIsNullWillThrowException() {
		Capabilities capabilities = mock(Capabilities.class);
		given(capabilities.getBrowserName()).willReturn("htmlunit");
		given(capabilities.getVersion()).willReturn("chrome");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new LocalHostWebConnectionHtmlUnitDriver(null, capabilities))
				.withMessageContaining("Environment must not be null");
	}

	@Test
	void getWhenUrlIsRelativeAndNoPortWillUseLocalhost8080() throws Exception {
		MockEnvironment environment = new MockEnvironment();
		LocalHostWebConnectionHtmlUnitDriver driver = new TestLocalHostWebConnectionHtmlUnitDriver(environment);
		driver.get("/test");
		then(this.webClient).should().getPage(any(WebWindow.class),
				requestToUrl(new URL("http://localhost:8080/test")));
	}

	@Test
	void getWhenUrlIsRelativeAndHasPortWillUseLocalhostPort() throws Exception {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("local.server.port", "8181");
		LocalHostWebConnectionHtmlUnitDriver driver = new TestLocalHostWebConnectionHtmlUnitDriver(environment);
		driver.get("/test");
		then(this.webClient).should().getPage(any(WebWindow.class),
				requestToUrl(new URL("http://localhost:8181/test")));
	}

	private WebRequest requestToUrl(URL url) {
		return argThat(new WebRequestUrlArgumentMatcher(url));
	}

	public class TestLocalHostWebConnectionHtmlUnitDriver extends LocalHostWebConnectionHtmlUnitDriver {

		TestLocalHostWebConnectionHtmlUnitDriver(Environment environment) {
			super(environment);
		}

		@Override
		public WebClient getWebClient() {
			return LocalHostWebConnectionHtmlUnitDriverTests.this.webClient;
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
