/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.web.htmlunit;

import java.io.IOException;

import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.Page;
import org.htmlunit.WebClient;

import org.springframework.util.Assert;
import org.springframework.web.util.UriBuilderFactory;

/**
 * HTML Unit {@link WebClient} supported by a {@link UriBuilderFactory}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class UriBuilderFactoryWebClient extends WebClient {

	private final UriBuilderFactory uriBuilderFactory;

	public UriBuilderFactoryWebClient(UriBuilderFactory uriBuilderFactory) {
		Assert.notNull(uriBuilderFactory, "'uriBuilderFactory' must not be null");
		this.uriBuilderFactory = uriBuilderFactory;
	}

	@Override
	public <P extends Page> P getPage(String url) throws IOException, FailingHttpStatusCodeException {
		return super.getPage(this.uriBuilderFactory.uriString(url).toUriString());
	}

}
