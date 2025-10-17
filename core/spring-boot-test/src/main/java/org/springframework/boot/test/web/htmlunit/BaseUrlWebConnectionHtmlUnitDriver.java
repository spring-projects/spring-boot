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

import org.htmlunit.BrowserVersion;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.Capabilities;

import org.springframework.boot.test.http.server.BaseUrl;
import org.springframework.boot.test.http.server.BaseUrlProvider;
import org.springframework.test.web.servlet.htmlunit.webdriver.WebConnectionHtmlUnitDriver;

/**
 * HTML Unit {@link WebConnectionHtmlUnitDriver} that will automatically prefix relative
 * URLs with a {@link BaseUrlProvider provided} {@link BaseUrl}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class BaseUrlWebConnectionHtmlUnitDriver extends WebConnectionHtmlUnitDriver {

	private @Nullable BaseUrl baseUrl;

	public BaseUrlWebConnectionHtmlUnitDriver(@Nullable BaseUrl baseUrl) {
		this.baseUrl = baseUrl;
	}

	public BaseUrlWebConnectionHtmlUnitDriver(@Nullable BaseUrl baseUrl, boolean enableJavascript) {
		super(enableJavascript);
		this.baseUrl = baseUrl;
	}

	public BaseUrlWebConnectionHtmlUnitDriver(@Nullable BaseUrl baseUrl, BrowserVersion browserVersion) {
		super(browserVersion);
		this.baseUrl = baseUrl;
	}

	public BaseUrlWebConnectionHtmlUnitDriver(@Nullable BaseUrl baseUrl, Capabilities capabilities) {
		super(capabilities);
		this.baseUrl = baseUrl;
	}

	@Override
	public void get(String url) {
		if (this.baseUrl != null) {
			url = this.baseUrl.getUriBuilderFactory().uriString(url).toUriString();
		}
		super.get(url);
	}

}
