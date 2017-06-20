/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.devtools.test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

/**
 * Mock {@link ClientHttpRequestFactory}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class MockClientHttpRequestFactory implements ClientHttpRequestFactory {

	private static final byte[] NO_DATA = {};

	private AtomicLong seq = new AtomicLong();

	private Deque<Object> responses = new ArrayDeque<>();

	private List<MockClientHttpRequest> executedRequests = new ArrayList<>();

	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod)
			throws IOException {
		return new MockRequest(uri, httpMethod);
	}

	public void willRespond(HttpStatus... response) {
		for (HttpStatus status : response) {
			this.responses.add(new Response(0, null, status));
		}
	}

	public void willRespond(IOException... response) {
		for (IOException exception : response) {
			this.responses.addLast(exception);
		}
	}

	public void willRespond(String... response) {
		for (String payload : response) {
			this.responses.add(new Response(0, payload.getBytes(), HttpStatus.OK));
		}
	}

	public void willRespondAfterDelay(int delay, HttpStatus status) {
		this.responses.add(new Response(delay, null, status));
	}

	public List<MockClientHttpRequest> getExecutedRequests() {
		return this.executedRequests;
	}

	private class MockRequest extends MockClientHttpRequest {

		MockRequest(URI uri, HttpMethod httpMethod) {
			super(httpMethod, uri);
		}

		@Override
		protected ClientHttpResponse executeInternal() throws IOException {
			MockClientHttpRequestFactory.this.executedRequests.add(this);
			Object response = MockClientHttpRequestFactory.this.responses.pollFirst();
			if (response instanceof IOException) {
				throw (IOException) response;
			}
			if (response == null) {
				response = new Response(0, null, HttpStatus.GONE);
			}
			return ((Response) response)
					.asHttpResponse(MockClientHttpRequestFactory.this.seq);
		}

	}

	static class Response {

		private final int delay;

		private final byte[] payload;

		private final HttpStatus status;

		Response(int delay, byte[] payload, HttpStatus status) {
			this.delay = delay;
			this.payload = payload;
			this.status = status;
		}

		public ClientHttpResponse asHttpResponse(AtomicLong seq) {
			MockClientHttpResponse httpResponse = new MockClientHttpResponse(
					this.payload == null ? NO_DATA : this.payload, this.status);
			waitForDelay();
			if (this.payload != null) {
				httpResponse.getHeaders().setContentLength(this.payload.length);
				httpResponse.getHeaders()
						.setContentType(MediaType.APPLICATION_OCTET_STREAM);
				httpResponse.getHeaders().add("x-seq",
						Long.toString(seq.incrementAndGet()));
			}
			return httpResponse;
		}

		private void waitForDelay() {
			if (this.delay > 0) {
				try {
					Thread.sleep(this.delay);
				}
				catch (InterruptedException ex) {
					// Ignore
				}
			}
		}

	}

}
