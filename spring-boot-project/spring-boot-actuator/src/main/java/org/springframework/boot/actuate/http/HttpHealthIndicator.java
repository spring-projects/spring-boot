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
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.HttpStatus;

/**
 * {@link HttpHealthIndicator} that tests the status of a get request to the configured
 * url returns a 2xx response.
 *
 * @author Harry Martland
 * @since 2.1.0
 */
public class HttpHealthIndicator extends AbstractHealthIndicator {

	private final HttpClient httpClient;
	private final String url;

	/**
	 * Create a new {@link HttpHealthIndicator} instance for a url using the provided
	 * {@link HttpClient}.
	 *
	 * @param httpClient the http client used to make the check request with
	 * @param url the url to be checked
	 */
	public HttpHealthIndicator(HttpClient httpClient, String url) {
		this.httpClient = httpClient;
		this.url = url;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) {
		HttpGet httpGet = new HttpGet(this.url);
		try {
			HttpResponse execute = this.httpClient.execute(httpGet);
			int statusCode = execute.getStatusLine().getStatusCode();
			if (isErrorRequest(statusCode)) {
				builder.withDetail("statusCode", statusCode);
				builder.down();
			}
			else {
				builder.up();
			}
		}
		catch (IOException e) {
			builder.down(e);
		}
		finally {
			httpGet.releaseConnection();
		}
	}

	private boolean isErrorRequest(int statusCode) {
		return !HttpStatus.valueOf(statusCode).is2xxSuccessful();
	}
}
