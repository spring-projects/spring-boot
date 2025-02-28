/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.http.client;

import java.time.Duration;

import org.springframework.boot.ssl.SslBundle;

/**
 * Settings that can be applied when creating a blocking or reactive HTTP client.
 *
 * @param redirects the follow redirect strategy to use or null to redirect whenever the
 * underlying library allows it
 * @param connectTimeout the connect timeout
 * @param readTimeout the read timeout
 * @param sslBundle the SSL bundle providing SSL configuration
 * @author Phillip Webb
 * @since 3.5.0
 */
public record HttpClientSettings(HttpRedirects redirects, Duration connectTimeout, Duration readTimeout,
		SslBundle sslBundle) {

	static final HttpClientSettings DEFAULTS = new HttpClientSettings(null, null, null, null);

	public HttpClientSettings {
		redirects = (redirects != null) ? redirects : HttpRedirects.FOLLOW_WHEN_POSSIBLE;
	}

}
