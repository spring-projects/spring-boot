/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.http;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.boot.actuate.health.Health;
import org.springframework.http.HttpStatus;

import static org.mockito.Mockito.never;

/**
 * Test for {@link HttpHealthIndicator}.
 *
 * @author Harry Martland
 * @since 2.1.0
 */
public class HttpHealthIndicatorTest {

	private HttpClient httpClient = Mockito.mock(HttpClient.class);
	private Health.Builder healthBuilder = Mockito.mock(Health.Builder.class);
	private HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
	private StatusLine statusLine = Mockito.mock(StatusLine.class);

	@Before
	public void setUp() throws IOException {
		Mockito.when(this.httpClient.execute(Mockito.any()))
				.thenReturn(this.httpResponse);
		Mockito.when(this.httpResponse.getStatusLine()).thenReturn(this.statusLine);
	}

	@Test
	public void shouldBeUpWhenRequestIs200() {
		HttpHealthIndicator httpHealthIndicator = new HttpHealthIndicator(this.httpClient,
				"http://test.com");

		Mockito.when(this.statusLine.getStatusCode()).thenReturn(HttpStatus.OK.value());

		httpHealthIndicator.doHealthCheck(this.healthBuilder);

		Mockito.verify(this.healthBuilder).up();
		Mockito.verify(this.healthBuilder, never()).down();
		Mockito.verify(this.healthBuilder, never()).down(Mockito.any());
	}

	@Test
	public void shouldBeDownWhenRequestIsNot2xx() {
		HttpHealthIndicator httpHealthIndicator = new HttpHealthIndicator(this.httpClient,
				"http://test.com");

		Mockito.when(this.statusLine.getStatusCode())
				.thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());

		httpHealthIndicator.doHealthCheck(this.healthBuilder);

		Mockito.verify(this.healthBuilder, never()).up();
		Mockito.verify(this.healthBuilder).down();
		Mockito.verify(this.healthBuilder, never()).down(Mockito.any());
		Mockito.verify(this.healthBuilder).withDetail("statusCode",
				HttpStatus.INTERNAL_SERVER_ERROR.value());
	}

	@Test
	public void shouldBeDownWhenExeptionIsThrown() throws IOException {
		HttpHealthIndicator httpHealthIndicator = new HttpHealthIndicator(this.httpClient,
				"http://test.com");

		IOException exception = new IOException("test error");
		Mockito.when(this.httpClient.execute(Mockito.any())).thenThrow(exception);

		httpHealthIndicator.doHealthCheck(this.healthBuilder);

		Mockito.verify(this.healthBuilder, never()).up();
		Mockito.verify(this.healthBuilder, never()).down();
		Mockito.verify(this.healthBuilder).down(exception);
	}
}
