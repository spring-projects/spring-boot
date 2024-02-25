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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import zipkin2.Call;
import zipkin2.Callback;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * An {@link HttpSender} which uses {@link RestTemplate} for HTTP communication.
 *
 * @author Moritz Halbritter
 * @author Stefan Bratanov
 */
class ZipkinRestTemplateSender extends HttpSender {

	private final String endpoint;

	private final RestTemplate restTemplate;

	/**
     * Constructs a new ZipkinRestTemplateSender with the specified endpoint and RestTemplate.
     *
     * @param endpoint the URL endpoint to send the Zipkin spans to
     * @param restTemplate the RestTemplate instance to use for sending the spans
     */
    ZipkinRestTemplateSender(String endpoint, RestTemplate restTemplate) {
		this.endpoint = endpoint;
		this.restTemplate = restTemplate;
	}

	/**
     * Sends the encoded spans in a batched format using a REST template.
     * 
     * @param batchedEncodedSpans the batched encoded spans to be sent
     * @return the HTTP POST call object used to send the spans
     */
    @Override
	public HttpPostCall sendSpans(byte[] batchedEncodedSpans) {
		return new RestTemplateHttpPostCall(this.endpoint, batchedEncodedSpans, this.restTemplate);
	}

	/**
     * RestTemplateHttpPostCall class.
     */
    private static class RestTemplateHttpPostCall extends HttpPostCall {

		private final String endpoint;

		private final RestTemplate restTemplate;

		/**
         * Sends a HTTP POST request to the specified endpoint using the provided body and RestTemplate.
         * 
         * @param endpoint the URL endpoint to send the request to
         * @param body the byte array representing the request body
         * @param restTemplate the RestTemplate instance to use for making the request
         */
        RestTemplateHttpPostCall(String endpoint, byte[] body, RestTemplate restTemplate) {
			super(body);
			this.endpoint = endpoint;
			this.restTemplate = restTemplate;
		}

		/**
         * Creates a clone of the RestTemplateHttpPostCall object.
         * 
         * @return a new instance of the RestTemplateHttpPostCall object with the same endpoint, uncompressed body, and restTemplate.
         */
        @Override
		public Call<Void> clone() {
			return new RestTemplateHttpPostCall(this.endpoint, getUncompressedBody(), this.restTemplate);
		}

		/**
         * Executes a POST request to the specified endpoint using the provided body and default headers.
         * 
         * @return null
         */
        @Override
		protected Void doExecute() {
			HttpEntity<byte[]> request = new HttpEntity<>(getBody(), getDefaultHeaders());
			this.restTemplate.exchange(this.endpoint, HttpMethod.POST, request, Void.class);
			return null;
		}

		/**
         * Executes the enqueue operation for the RestTemplateHttpPostCall.
         * 
         * @param callback the callback to be invoked after the enqueue operation is completed
         */
        @Override
		protected void doEnqueue(Callback<Void> callback) {
			try {
				doExecute();
				callback.onSuccess(null);
			}
			catch (Exception ex) {
				callback.onError(ex);
			}
		}

	}

}
