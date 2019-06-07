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

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpRequestFactory} to apply a given HTTP Basic Authentication
 * username/password pair, unless a custom Authorization header has been set before.
 *
 * @author Dmytro Nosan
 */
class BasicAuthenticationClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

	private final BasicAuthentication authentication;

	BasicAuthenticationClientHttpRequestFactory(BasicAuthentication authentication,
			ClientHttpRequestFactory clientHttpRequestFactory) {
		super(clientHttpRequestFactory);
		Assert.notNull(authentication, "Authentication must not be null");
		this.authentication = authentication;
	}

	@Override
	protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory)
			throws IOException {
		ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
		this.authentication.applyTo(request);
		return request;
	}

}
