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

package org.springframework.boot.webservices.client;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;

/**
 * {@link WebServiceMessageSender} builder that can detect a suitable HTTP library based
 * on the classpath.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
public class HttpWebServiceMessageSenderBuilder {

	private Duration connectTimeout;

	private Duration readTimeout;

	private Function<ClientHttpRequestFactorySettings, ClientHttpRequestFactory> requestFactory;

	/**
	 * Set the connection timeout.
	 * @param connectTimeout the connection timeout
	 * @return a new builder instance
	 */
	public HttpWebServiceMessageSenderBuilder setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
		return this;
	}

	/**
	 * Set the read timeout.
	 * @param readTimeout the read timeout
	 * @return a new builder instance
	 */
	public HttpWebServiceMessageSenderBuilder setReadTimeout(Duration readTimeout) {
		this.readTimeout = readTimeout;
		return this;
	}

	/**
	 * Set the {@code Supplier} of {@link ClientHttpRequestFactory} that should be called
	 * to create the HTTP-based {@link WebServiceMessageSender}.
	 * @param requestFactorySupplier the supplier for the request factory
	 * @return a new builder instance
	 */
	public HttpWebServiceMessageSenderBuilder requestFactory(
			Supplier<ClientHttpRequestFactory> requestFactorySupplier) {
		Assert.notNull(requestFactorySupplier, "RequestFactorySupplier must not be null");
		this.requestFactory = (settings) -> ClientHttpRequestFactories.get(requestFactorySupplier, settings);
		return this;
	}

	/**
	 * Set the {@code Function} of {@link ClientHttpRequestFactorySettings} to
	 * {@link ClientHttpRequestFactory} that should be called to create the HTTP-based
	 * {@link WebServiceMessageSender}.
	 * @param requestFactoryFunction the function for the request factory
	 * @return a new builder instance
	 * @since 3.0.0
	 */
	public HttpWebServiceMessageSenderBuilder requestFactory(
			Function<ClientHttpRequestFactorySettings, ClientHttpRequestFactory> requestFactoryFunction) {
		Assert.notNull(requestFactoryFunction, "RequestFactoryFunction must not be null");
		this.requestFactory = requestFactoryFunction;
		return this;
	}

	/**
	 * Build the {@link WebServiceMessageSender} instance.
	 * @return the {@link WebServiceMessageSender} instance
	 */
	public WebServiceMessageSender build() {
		return new ClientHttpRequestMessageSender(getRequestFactory());
	}

	private ClientHttpRequestFactory getRequestFactory() {
		ClientHttpRequestFactorySettings settings = new ClientHttpRequestFactorySettings(this.connectTimeout,
				this.readTimeout, null);
		return (this.requestFactory != null) ? this.requestFactory.apply(settings)
				: ClientHttpRequestFactories.get(settings);
	}

}
