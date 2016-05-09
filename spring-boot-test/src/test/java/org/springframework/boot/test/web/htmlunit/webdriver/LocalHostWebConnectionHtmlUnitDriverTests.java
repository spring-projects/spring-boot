/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.Capabilities;

import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LocalHostWebConnectionHtmlUnitDriver}.
 *
 * @author Phillip Webb
 */
public class LocalHostWebConnectionHtmlUnitDriverTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private WebClient webClient;

	public LocalHostWebConnectionHtmlUnitDriverTests() {
		MockitoAnnotations.initMocks(this);
		given(this.webClient.getOptions()).willReturn(new WebClientOptions());
	}

	@Test
	public void createWhenEnvironmentIsNullWillThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Environment must not be null");
		new LocalHostWebConnectionHtmlUnitDriver(null);
	}

	@Test
	public void createWithJavascriptFlagWhenEnvironmentIsNullWillThrowException()
			throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Environment must not be null");
		new LocalHostWebConnectionHtmlUnitDriver(null, true);
	}

	@Test
	public void createWithBrowserVersionWhenEnvironmentIsNullWillThrowException()
			throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Environment must not be null");
		new LocalHostWebConnectionHtmlUnitDriver(null, BrowserVersion.CHROME);
	}

	@Test
	public void createWithCapabilitiesWhenEnvironmentIsNullWillThrowException()
			throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Environment must not be null");
		new LocalHostWebConnectionHtmlUnitDriver(null, mock(Capabilities.class));
	}

	@Test
	public void getWhenUrlIsRelativeAndNoPortWillUseLocalhost8080() throws Exception {
		MockEnvironment environment = new MockEnvironment();
		LocalHostWebConnectionHtmlUnitDriver driver = new TestLocalHostWebConnectionHtmlUnitDriver(
				environment);
		driver.get("/test");
		verify(this.webClient).getPage(new URL("http://localhost:8080/test"));
	}

	@Test
	public void getWhenUrlIsRelativeAndHasPortWillUseLocalhostPort() throws Exception {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("local.server.port", "8181");
		LocalHostWebConnectionHtmlUnitDriver driver = new TestLocalHostWebConnectionHtmlUnitDriver(
				environment);
		driver.get("/test");
		verify(this.webClient).getPage(new URL("http://localhost:8181/test"));
	}

	public class TestLocalHostWebConnectionHtmlUnitDriver
			extends LocalHostWebConnectionHtmlUnitDriver {

		public TestLocalHostWebConnectionHtmlUnitDriver(Environment environment) {
			super(environment);
		}

		@Override
		public WebClient getWebClient() {
			return LocalHostWebConnectionHtmlUnitDriverTests.this.webClient;
		}

	}

}
