/*
 * Copyright 2012-2024 the original author or authors.
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

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;

/**
 * Factory that can be used to create a {@link WebServiceMessageSender}.
 *
 * @author Phillip Webb
 * @since 3.4.0
 */
@FunctionalInterface
public interface WebServiceMessageSenderFactory {

	/**
	 * Return a new {@link WebServiceMessageSender} instance.
	 * @return the web service message sender
	 */
	WebServiceMessageSender getWebServiceMessageSender();

	/**
	 * Returns a factory that will create a {@link ClientHttpRequestMessageSender} backed
	 * by a detected {@link ClientHttpRequestFactory}.
	 * @return a new {@link WebServiceMessageSenderFactory}
	 */
	static WebServiceMessageSenderFactory http() {
		return http(ClientHttpRequestFactoryBuilder.detect(), null);
	}

	/**
	 * Returns a factory that will create a {@link ClientHttpRequestMessageSender} backed
	 * by a detected {@link ClientHttpRequestFactory}.
	 * @param requestFactorySettings the setting to apply
	 * @return a new {@link WebServiceMessageSenderFactory}
	 */
	static WebServiceMessageSenderFactory http(ClientHttpRequestFactorySettings requestFactorySettings) {
		return http(ClientHttpRequestFactoryBuilder.detect(), requestFactorySettings);
	}

	/**
	 * Returns a factory that will create a {@link ClientHttpRequestMessageSender} backed
	 * by a {@link ClientHttpRequestFactory} created from the given
	 * {@link ClientHttpRequestFactoryBuilder}.
	 * @param requestFactoryBuilder the request factory builder to use
	 * @param requestFactorySettings the settings to apply
	 * @return a new {@link WebServiceMessageSenderFactory}
	 */
	static WebServiceMessageSenderFactory http(ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder,
			ClientHttpRequestFactorySettings requestFactorySettings) {
		Assert.notNull(requestFactoryBuilder, "'requestFactoryBuilder' must not be null");
		return () -> new ClientHttpRequestMessageSender(requestFactoryBuilder.build(requestFactorySettings));
	}

}
