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
import org.openqa.selenium.Capabilities;

import org.springframework.test.web.servlet.htmlunit.webdriver.WebConnectionHtmlUnitDriver;
import org.springframework.util.Assert;
import org.springframework.web.util.UriBuilderFactory;

/**
 * HTML Unit {@link WebConnectionHtmlUnitDriver} supported by a {@link UriBuilderFactory}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class UriBuilderFactoryWebConnectionHtmlUnitDriver extends WebConnectionHtmlUnitDriver {

	private final UriBuilderFactory uriBuilderFactory;

	public UriBuilderFactoryWebConnectionHtmlUnitDriver(UriBuilderFactory uriBuilderFactory) {
		Assert.notNull(uriBuilderFactory, "'uriBuilderFactory' must not be null");
		this.uriBuilderFactory = uriBuilderFactory;
	}

	public UriBuilderFactoryWebConnectionHtmlUnitDriver(UriBuilderFactory uriBuilderFactory, boolean enableJavascript) {
		super(enableJavascript);
		Assert.notNull(uriBuilderFactory, "'uriBuilderFactory' must not be null");
		this.uriBuilderFactory = uriBuilderFactory;
	}

	public UriBuilderFactoryWebConnectionHtmlUnitDriver(UriBuilderFactory uriBuilderFactory,
			BrowserVersion browserVersion) {
		super(browserVersion);
		Assert.notNull(uriBuilderFactory, "'uriBuilderFactory' must not be null");
		this.uriBuilderFactory = uriBuilderFactory;
	}

	public UriBuilderFactoryWebConnectionHtmlUnitDriver(UriBuilderFactory uriBuilderFactory,
			Capabilities capabilities) {
		super(capabilities);
		Assert.notNull(uriBuilderFactory, "'uriBuilderFactory' must not be null");
		this.uriBuilderFactory = uriBuilderFactory;
	}

	@Override
	@SuppressWarnings("ConstantValue") // default constructor calls this method
	public void get(String url) {
		super.get((this.uriBuilderFactory != null) ? this.uriBuilderFactory.uriString(url).toUriString() : url);
	}

}
