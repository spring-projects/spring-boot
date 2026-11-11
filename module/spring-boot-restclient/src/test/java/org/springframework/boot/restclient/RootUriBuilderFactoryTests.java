/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;
import org.springframework.web.util.UriTemplateHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RootUriBuilderFactory}.
 *
 * @author Scott Frederick
 */
@SuppressWarnings("removal")
class RootUriBuilderFactoryTests {

	@Test
	void uriStringPrefixesRoot() throws URISyntaxException {
		UriBuilderFactory builderFactory = new RootUriBuilderFactory("https://example.com",
				mock(UriTemplateHandler.class));
		UriBuilder builder = builderFactory.uriString("/hello");
		assertThat(builder.build()).isEqualTo(new URI("https://example.com/hello"));
	}

}
