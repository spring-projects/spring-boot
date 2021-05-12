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

package org.springframework.boot.webservices.client;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.boot.web.client.ClientHttpRequestFactorySupplier;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
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

	private Supplier<ClientHttpRequestFactory> requestFactorySupplier;

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
		Assert.notNull(requestFactorySupplier, "RequestFactory Supplier must not be null");
		this.requestFactorySupplier = requestFactorySupplier;
		return this;
	}

	public WebServiceMessageSender build() {
		ClientHttpRequestFactory requestFactory = (this.requestFactorySupplier != null)
				? this.requestFactorySupplier.get() : new ClientHttpRequestFactorySupplier().get();
		if (this.connectTimeout != null) {
			new TimeoutRequestFactoryCustomizer(this.connectTimeout, "setConnectTimeout").customize(requestFactory);
		}
		if (this.readTimeout != null) {
			new TimeoutRequestFactoryCustomizer(this.readTimeout, "setReadTimeout").customize(requestFactory);
		}
		return new ClientHttpRequestMessageSender(requestFactory);
	}

	/**
	 * {@link ClientHttpRequestFactory} customizer to call a "set timeout" method.
	 */
	private static class TimeoutRequestFactoryCustomizer {

		private final Duration timeout;

		private final String methodName;

		TimeoutRequestFactoryCustomizer(Duration timeout, String methodName) {
			this.timeout = timeout;
			this.methodName = methodName;
		}

		void customize(ClientHttpRequestFactory factory) {
			ReflectionUtils.invokeMethod(findMethod(factory), factory, Math.toIntExact(this.timeout.toMillis()));
		}

		private Method findMethod(ClientHttpRequestFactory factory) {
			Method method = ReflectionUtils.findMethod(factory.getClass(), this.methodName, int.class);
			if (method != null) {
				return method;
			}
			throw new IllegalStateException(
					"Request factory " + factory.getClass() + " does not have a " + this.methodName + "(int) method");
		}

	}

}
