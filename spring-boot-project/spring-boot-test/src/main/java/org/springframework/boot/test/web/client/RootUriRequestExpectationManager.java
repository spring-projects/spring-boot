/*
 * Copyright 2012-2022 the original author or authors.
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

	/**
     * Constructs a new RootUriRequestExpectationManager with the specified root URI and expectation manager.
     * 
     * @param rootUri the root URI for the request expectation manager (must not be null)
     * @param expectationManager the expectation manager to be used (must not be null)
     * @throws IllegalArgumentException if either rootUri or expectationManager is null
     */
    public RootUriRequestExpectationManager(String rootUri, RequestExpectationManager expectationManager) {
		Assert.notNull(rootUri, "RootUri must not be null");
		Assert.notNull(expectationManager, "ExpectationManager must not be null");
		this.rootUri = rootUri;
		this.expectationManager = expectationManager;
	}

	/**
     * Sets the expectation for the specified number of requests that match the given request matcher.
     * 
     * @param count The expected count of requests.
     * @param requestMatcher The request matcher to be used for matching requests.
     * @return The response actions for the expected request.
     */
    @Override
	public ResponseActions expectRequest(ExpectedCount count, RequestMatcher requestMatcher) {
		return this.expectationManager.expectRequest(count, requestMatcher);
	}

	/**
     * Validates the request by checking if the URI starts with the root URI. If it does, the URI is replaced with the
     * substring after the root URI. Then, the request is passed to the expectation manager for further validation.
     * If an AssertionError occurs during validation, it is checked if the error message starts with "Request URI expected:</".
     * If it does, the root URI is appended to the error message and a new AssertionError is thrown.
     * Otherwise, the original AssertionError is re-thrown.
     *
     * @param request the client HTTP request to be validated
     * @return the client HTTP response after validation
     * @throws IOException if an I/O error occurs during the validation process
     */
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

	/**
     * Replaces the URI of the given request with the specified replacement URI.
     * 
     * @param request The original request to be modified.
     * @param replacementUri The replacement URI to be set.
     * @return The modified request with the replaced URI.
     * @throws IllegalStateException if the replacement URI is not a valid URI.
     */
    private ClientHttpRequest replaceURI(ClientHttpRequest request, String replacementUri) {
		URI uri;
		try {
			uri = new URI(replacementUri);
			if (request instanceof MockClientHttpRequest mockClientHttpRequest) {
				mockClientHttpRequest.setURI(uri);
				return mockClientHttpRequest;
			}
			return new ReplaceUriClientHttpRequest(uri, request);
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
     * Verifies the expectations of the RootUriRequestExpectationManager.
     * This method calls the verify() method of the expectationManager object.
     */
    @Override
	public void verify() {
		this.expectationManager.verify();
	}

	/**
     * Verifies the expectations of the RootUriRequestExpectationManager within the specified timeout duration.
     *
     * @param timeout the duration within which the expectations should be verified
     */
    @Override
	public void verify(Duration timeout) {
		this.expectationManager.verify(timeout);
	}

	/**
     * Resets the RootUriRequestExpectationManager by resetting the expectation manager.
     */
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
		if (templateHandler instanceof RootUriTemplateHandler rootHandler) {
			return new RootUriRequestExpectationManager(rootHandler.getRootUri(), expectationManager);
		}
		return expectationManager;
	}

	/**
	 * {@link ClientHttpRequest} wrapper to replace the request URI.
	 */
	private static class ReplaceUriClientHttpRequest extends HttpRequestWrapper implements ClientHttpRequest {

		private final URI uri;

		/**
         * Constructs a new ReplaceUriClientHttpRequest with the specified URI and ClientHttpRequest.
         * 
         * @param uri the URI to be replaced
         * @param request the ClientHttpRequest to be used
         */
        ReplaceUriClientHttpRequest(URI uri, ClientHttpRequest request) {
			super(request);
			this.uri = uri;
		}

		/**
         * Returns the URI of the client HTTP request.
         *
         * @return the URI of the client HTTP request
         */
        @Override
		public URI getURI() {
			return this.uri;
		}

		/**
         * Returns the output stream of the request body.
         *
         * @return the output stream of the request body
         * @throws IOException if an I/O error occurs
         */
        @Override
		public OutputStream getBody() throws IOException {
			return getRequest().getBody();
		}

		/**
         * Executes the HTTP request and returns the response.
         *
         * @return the response of the HTTP request
         * @throws IOException if an I/O error occurs while executing the request
         */
        @Override
		public ClientHttpResponse execute() throws IOException {
			return getRequest().execute();
		}

		/**
         * Returns the underlying ClientHttpRequest object.
         *
         * @return the underlying ClientHttpRequest object
         */
        @Override
		public ClientHttpRequest getRequest() {
			return (ClientHttpRequest) super.getRequest();
		}

	}

}
