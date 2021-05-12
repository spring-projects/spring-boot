/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.test.web.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.MockRestServiceServer.MockRestServiceServerBuilder;
import org.springframework.test.web.client.RequestExpectationManager;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.SimpleRequestExpectationManager;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

/**
 * {@link RequestExpectationManager} that strips the specified root URI from the request
 * before verification. Can be used to simply test declarations when all REST calls start
 * the same way. For example: <pre class="code">
 * RestTemplate restTemplate = new RestTemplateBuilder().rootUri("https://example.com").build();
 * MockRestServiceServer server = RootUriRequestExpectationManager.bindTo(restTemplate);
 * server.expect(requestTo("/hello")).andRespond(withSuccess());
 * restTemplate.getForEntity("/hello", String.class);
 * </pre>
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see RootUriTemplateHandler
 * @see #bindTo(RestTemplate)
 * @see #forRestTemplate(RestTemplate, RequestExpectationManager)
 */
public class RootUriRequestExpectationManager implements RequestExpectationManager {

	private final String rootUri;

	private final RequestExpectationManager expectationManager;

	public RootUriRequestExpectationManager(String rootUri, RequestExpectationManager expectationManager) {
		Assert.notNull(rootUri, "RootUri must not be null");
		Assert.notNull(expectationManager, "ExpectationManager must not be null");
		this.rootUri = rootUri;
		this.expectationManager = expectationManager;
	}

	@Override
	public ResponseActions expectRequest(ExpectedCount count, RequestMatcher requestMatcher) {
		return this.expectationManager.expectRequest(count, requestMatcher);
	}

	@Override
	public ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException {
		String uri = request.getURI().toString();
		if (uri.startsWith(this.rootUri)) {
			request = replaceURI(request, uri.substring(this.rootUri.length()));
		}
		try {
			return this.expectationManager.validateRequest(request);
		}
		catch (AssertionError ex) {
			String message = ex.getMessage();
			String prefix = "Request URI expected:</";
			if (message != null && message.startsWith(prefix)) {
				throw new AssertionError(
						"Request URI expected:<" + this.rootUri + message.substring(prefix.length() - 1));
			}
			throw ex;
		}
	}

	private ClientHttpRequest replaceURI(ClientHttpRequest request, String replacementUri) {
		URI uri;
		try {
			uri = new URI(replacementUri);
			if (request instanceof MockClientHttpRequest) {
				((MockClientHttpRequest) request).setURI(uri);
				return request;
			}
			return new ReplaceUriClientHttpRequest(uri, request);
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public void verify() {
		this.expectationManager.verify();
	}

	@Override
	public void verify(Duration timeout) {
		this.expectationManager.verify(timeout);
	}

	@Override
	public void reset() {
		this.expectationManager.reset();
	}

	/**
	 * Return a bound {@link MockRestServiceServer} for the given {@link RestTemplate},
	 * configured with {@link RootUriRequestExpectationManager} when possible.
	 * @param restTemplate the source REST template
	 * @return a configured {@link MockRestServiceServer}
	 */
	public static MockRestServiceServer bindTo(RestTemplate restTemplate) {
		return bindTo(restTemplate, new SimpleRequestExpectationManager());
	}

	/**
	 * Return a bound {@link MockRestServiceServer} for the given {@link RestTemplate},
	 * configured with {@link RootUriRequestExpectationManager} when possible.
	 * @param restTemplate the source REST template
	 * @param expectationManager the source {@link RequestExpectationManager}
	 * @return a configured {@link MockRestServiceServer}
	 */
	public static MockRestServiceServer bindTo(RestTemplate restTemplate,
			RequestExpectationManager expectationManager) {
		MockRestServiceServerBuilder builder = MockRestServiceServer.bindTo(restTemplate);
		return builder.build(forRestTemplate(restTemplate, expectationManager));
	}

	/**
	 * Return {@link RequestExpectationManager} to be used for binding with the specified
	 * {@link RestTemplate}. If the {@link RestTemplate} is using a
	 * {@link RootUriTemplateHandler} then a {@link RootUriRequestExpectationManager} is
	 * returned, otherwise the source manager is returned unchanged.
	 * @param restTemplate the source REST template
	 * @param expectationManager the source {@link RequestExpectationManager}
	 * @return a {@link RequestExpectationManager} to be bound to the template
	 */
	public static RequestExpectationManager forRestTemplate(RestTemplate restTemplate,
			RequestExpectationManager expectationManager) {
		Assert.notNull(restTemplate, "RestTemplate must not be null");
		UriTemplateHandler templateHandler = restTemplate.getUriTemplateHandler();
		if (templateHandler instanceof RootUriTemplateHandler) {
			return new RootUriRequestExpectationManager(((RootUriTemplateHandler) templateHandler).getRootUri(),
					expectationManager);
		}
		return expectationManager;
	}

	/**
	 * {@link ClientHttpRequest} wrapper to replace the request URI.
	 */
	private static class ReplaceUriClientHttpRequest extends HttpRequestWrapper implements ClientHttpRequest {

		private final URI uri;

		ReplaceUriClientHttpRequest(URI uri, ClientHttpRequest request) {
			super(request);
			this.uri = uri;
		}

		@Override
		public URI getURI() {
			return this.uri;
		}

		@Override
		public OutputStream getBody() throws IOException {
			return getRequest().getBody();
		}

		@Override
		public ClientHttpResponse execute() throws IOException {
			return getRequest().execute();
		}

		@Override
		public ClientHttpRequest getRequest() {
			return (ClientHttpRequest) super.getRequest();
		}

	}

}
