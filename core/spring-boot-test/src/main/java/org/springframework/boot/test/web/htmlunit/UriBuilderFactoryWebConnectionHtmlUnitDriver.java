/*
 * Copyright 2012-present the original author or authors.
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
