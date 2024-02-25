/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import java.time.Duration;

import reactor.core.publisher.Mono;
import zipkin2.Call;
import zipkin2.Callback;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * An {@link HttpSender} which uses {@link WebClient} for HTTP communication.
 *
 * @author Stefan Bratanov
 * @author Moritz Halbritter
 */
class ZipkinWebClientSender extends HttpSender {

	private final String endpoint;

	private final WebClient webClient;

	private final Duration timeout;

	/**
     * Constructs a new ZipkinWebClientSender with the specified endpoint, WebClient, and timeout.
     *
     * @param endpoint the URL endpoint to send the Zipkin spans to
     * @param webClient the WebClient instance to use for sending the spans
     * @param timeout the maximum duration to wait for a response from the endpoint
     */
    ZipkinWebClientSender(String endpoint, WebClient webClient, Duration timeout) {
		this.endpoint = endpoint;
		this.webClient = webClient;
		this.timeout = timeout;
	}

	/**
     * Sends the encoded spans in a batched format using HTTP POST call.
     * 
     * @param batchedEncodedSpans the batched encoded spans to be sent
     * @return the HTTP POST call object used to send the spans
     */
    @Override
	public HttpPostCall sendSpans(byte[] batchedEncodedSpans) {
		return new WebClientHttpPostCall(this.endpoint, batchedEncodedSpans, this.webClient, this.timeout);
	}

	/**
     * WebClientHttpPostCall class.
     */
    private static class WebClientHttpPostCall extends HttpPostCall {

		private final String endpoint;

		private final WebClient webClient;

		private final Duration timeout;

		/**
         * Sends an HTTP POST request to the specified endpoint using the provided WebClient.
         * 
         * @param endpoint the URL or URI of the endpoint to send the request to
         * @param body the body of the request as a byte array
         * @param webClient the WebClient instance to use for sending the request
         * @param timeout the maximum duration to wait for the request to complete
         */
        WebClientHttpPostCall(String endpoint, byte[] body, WebClient webClient, Duration timeout) {
			super(body);
			this.endpoint = endpoint;
			this.webClient = webClient;
			this.timeout = timeout;
		}

		/**
         * Creates a clone of the WebClientHttpPostCall object.
         * 
         * @return A Call object representing the cloned WebClientHttpPostCall.
         */
        @Override
		public Call<Void> clone() {
			return new WebClientHttpPostCall(this.endpoint, getUncompressedBody(), this.webClient, this.timeout);
		}

		/**
         * Executes the HTTP POST call using the WebClient.
         * 
         * @return The result of the HTTP POST call.
         * @throws Exception if an error occurs during the execution of the HTTP POST call.
         */
        @Override
		protected Void doExecute() {
			sendRequest().block();
			return null;
		}

		/**
         * Enqueues a request to be sent using the WebClientHttpPostCall.
         * 
         * @param callback the callback to be invoked when the request is completed
         */
        @Override
		protected void doEnqueue(Callback<Void> callback) {
			sendRequest().subscribe((entity) -> callback.onSuccess(null), callback::onError);
		}

		/**
         * Sends a POST request to the specified endpoint using WebClient.
         * 
         * @return a Mono object representing the response entity with no body
         */
        private Mono<ResponseEntity<Void>> sendRequest() {
			return this.webClient.post()
				.uri(this.endpoint)
				.headers(this::addDefaultHeaders)
				.bodyValue(getBody())
				.retrieve()
				.toBodilessEntity()
				.timeout(this.timeout);
		}

		/**
         * Adds default headers to the provided HttpHeaders object.
         * 
         * @param headers the HttpHeaders object to which the default headers will be added
         */
        private void addDefaultHeaders(HttpHeaders headers) {
			headers.addAll(getDefaultHeaders());
		}

	}

}
