/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.web.htmlunit;

import java.io.IOException;
import java.net.URL;

import org.htmlunit.StringWebResponse;
import org.htmlunit.WebClient;
import org.htmlunit.WebConnection;
import org.htmlunit.WebResponse;
import org.junit.jupiter.api.Test;

import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UriBuilderFactoryWebClient}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
class UriBuilderFactoryWebClientTests {

	@Test
	void getPageWhenUriIsRelativeUsesBuilder() throws Exception {
		WebClient client = new UriBuilderFactoryWebClient(new DefaultUriBuilderFactory("https://localhost:8080"));
		WebConnection connection = mockConnection();
		client.setWebConnection(connection);
		client.getPage("/test");
		thenConnectionRequests(connection, new URL("https://localhost:8080/test"));
	}

	@Test
	void getPageWhenUriIsNotRelativeUsesUri() throws Exception {
		WebClient client = new UriBuilderFactoryWebClient(new DefaultUriBuilderFactory("https://localhost"));
		WebConnection connection = mockConnection();
		client.setWebConnection(connection);
		client.getPage("https://example.com:9000/test");
		thenConnectionRequests(connection, new URL("https://example.com:9000/test"));
	}

	private void thenConnectionRequests(WebConnection connection, URL url) throws IOException {
		then(connection).should().getResponse(assertArg((request) -> assertThat(request.getUrl()).isEqualTo(url)));
	}

	private WebConnection mockConnection() throws IOException {
		WebConnection connection = mock(WebConnection.class);
		WebResponse response = new StringWebResponse("test", new URL("http://localhost"));
		given(connection.getResponse(any())).willReturn(response);
		return connection;
	}

}
