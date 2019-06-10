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

package org.springframework.boot.web.client;

import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;

/**
 * A {@link AbstractHttpHeadersDefaultingCustomizer} that uses provided
 * {@link HttpHeaders} instance as default headers.
 *
 * @author Ilya Lukyanovich
 * @see HttpHeadersCustomizingClientHttpRequestFactory
 */
public class SimpleHttpHeaderDefaultingCustomizer extends AbstractHttpHeadersDefaultingCustomizer {

	private final HttpHeaders httpHeaders;

	public SimpleHttpHeaderDefaultingCustomizer(HttpHeaders httpHeaders) {
		Assert.notNull(httpHeaders, "Header must not be null");
		this.httpHeaders = httpHeaders;
	}

	@Override
	protected HttpHeaders createHeaders() {
		return new HttpHeaders(new LinkedMultiValueMap<>(this.httpHeaders));
	}

	/**
	 * A factory method that creates a {@link SimpleHttpHeaderDefaultingCustomizer} with a
	 * single header and a single value.
	 * @param header the header
	 * @param value the value
	 * @return new {@link SimpleHttpHeaderDefaultingCustomizer} instance
	 * @see HttpHeaders#set(String, String)
	 */
	public static HttpHeadersCustomizer singleHeader(@NonNull String header, @NonNull String value) {
		Assert.notNull(header, "Header must not be null empty");
		Assert.notNull(value, "Value must not be null empty");
		HttpHeaders headers = new HttpHeaders();
		headers.set(header, value);
		return new SimpleHttpHeaderDefaultingCustomizer(headers);
	}

	/**
	 * A factory method that creates a {@link SimpleHttpHeaderDefaultingCustomizer} for
	 * {@link HttpHeaders#AUTHORIZATION} header with pre-defined username and password
	 * pair.
	 * @param username the username
	 * @param password the password
	 * @return new {@link SimpleHttpHeaderDefaultingCustomizer} instance
	 * @see #basicAuthentication(String, String, Charset)
	 */
	public static HttpHeadersCustomizer basicAuthentication(@NonNull String username, @NonNull String password) {
		return basicAuthentication(username, password, null);
	}

	/**
	 * A factory method that creates a {@link SimpleHttpHeaderDefaultingCustomizer} for
	 * {@link HttpHeaders#AUTHORIZATION} header with pre-defined username and password
	 * pair.
	 * @param username the username
	 * @param password the password
	 * @param charset the header encoding charset
	 * @return new {@link SimpleHttpHeaderDefaultingCustomizer} instance
	 * @see HttpHeaders#setBasicAuth(String, String, Charset)
	 */
	public static HttpHeadersCustomizer basicAuthentication(@NonNull String username, @NonNull String password,
			@Nullable Charset charset) {
		Assert.notNull(username, "Username must not be null");
		Assert.notNull(password, "Password must not be null");
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(username, password, charset);
		return new SimpleHttpHeaderDefaultingCustomizer(headers);
	}

}
