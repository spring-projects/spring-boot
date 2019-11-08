/*
 * Copyright 2012-2019 the original author or authors.
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

import com.gargoylesoftware.htmlunit.StringWebResponse;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LocalHostWebClient}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
class LocalHostWebClientTests {

	@Captor
	private ArgumentCaptor<WebRequest> requestCaptor;

	LocalHostWebClientTests() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void createWhenEnvironmentIsNullWillThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new LocalHostWebClient(null))
				.withMessageContaining("Environment must not be null");
	}

	@Test
	void getPageWhenUrlIsRelativeAndNoPortWillUseLocalhost8080() throws Exception {
		MockEnvironment environment = new MockEnvironment();
		WebClient client = new LocalHostWebClient(environment);
		WebConnection connection = mockConnection();
		client.setWebConnection(connection);
		client.getPage("/test");
		verify(connection).getResponse(this.requestCaptor.capture());
		assertThat(this.requestCaptor.getValue().getUrl()).isEqualTo(new URL("http://localhost:8080/test"));
	}

	@Test
	void getPageWhenUrlIsRelativeAndHasPortWillUseLocalhostPort() throws Exception {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("local.server.port", "8181");
		WebClient client = new LocalHostWebClient(environment);
		WebConnection connection = mockConnection();
		client.setWebConnection(connection);
		client.getPage("/test");
		verify(connection).getResponse(this.requestCaptor.capture());
		assertThat(this.requestCaptor.getValue().getUrl()).isEqualTo(new URL("http://localhost:8181/test"));
	}

	private WebConnection mockConnection() throws IOException {
		WebConnection connection = mock(WebConnection.class);
		WebResponse response = new StringWebResponse("test", new URL("http://localhost"));
		given(connection.getResponse(any())).willReturn(response);
		return connection;
	}

}
