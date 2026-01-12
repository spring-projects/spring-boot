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
