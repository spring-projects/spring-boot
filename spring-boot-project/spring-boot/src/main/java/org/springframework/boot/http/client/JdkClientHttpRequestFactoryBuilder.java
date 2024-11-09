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

package org.springframework.boot.http.client;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.net.ssl.SSLParameters;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Builder for {@link ClientHttpRequestFactoryBuilder#jdk()}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.4.0
 */
public class JdkClientHttpRequestFactoryBuilder
		extends AbstractClientHttpRequestFactoryBuilder<JdkClientHttpRequestFactory> {

	private final Consumer<HttpClient.Builder> httpClientCustomizer;

	JdkClientHttpRequestFactoryBuilder() {
		this(null, emptyCustomizer());
	}

	private JdkClientHttpRequestFactoryBuilder(List<Consumer<JdkClientHttpRequestFactory>> customizers,
			Consumer<HttpClient.Builder> httpClientCustomizer) {
		super(customizers);
		this.httpClientCustomizer = httpClientCustomizer;
	}

	@Override
	public JdkClientHttpRequestFactoryBuilder withCustomizer(Consumer<JdkClientHttpRequestFactory> customizer) {
		return new JdkClientHttpRequestFactoryBuilder(mergedCustomizers(customizer), this.httpClientCustomizer);
	}

	@Override
	public JdkClientHttpRequestFactoryBuilder withCustomizers(
			Collection<Consumer<JdkClientHttpRequestFactory>> customizers) {
		return new JdkClientHttpRequestFactoryBuilder(mergedCustomizers(customizers), this.httpClientCustomizer);
	}

	/**
	 * Return a new {@link JdkClientHttpRequestFactoryBuilder} that applies additional
	 * customization to the underlying {@link java.net.http.HttpClient.Builder}.
	 * @param httpClientCustomizer the customizer to apply
	 * @return a new {@link JdkClientHttpRequestFactoryBuilder} instance
	 */
	public JdkClientHttpRequestFactoryBuilder withHttpClientCustomizer(
			Consumer<HttpClient.Builder> httpClientCustomizer) {
		Assert.notNull(httpClientCustomizer, "'httpClientCustomizer' must not be null");
		return new JdkClientHttpRequestFactoryBuilder(getCustomizers(),
				this.httpClientCustomizer.andThen(httpClientCustomizer));
	}

	@Override
	protected JdkClientHttpRequestFactory createClientHttpRequestFactory(ClientHttpRequestFactorySettings settings) {
		HttpClient httpClient = createHttpClient(settings);
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::readTimeout).to(requestFactory::setReadTimeout);
		return requestFactory;
	}

	private HttpClient createHttpClient(ClientHttpRequestFactorySettings settings) {
		HttpClient.Builder builder = HttpClient.newBuilder();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::connectTimeout).to(builder::connectTimeout);
		map.from(settings::sslBundle).as(SslBundle::createSslContext).to(builder::sslContext);
		map.from(settings::sslBundle).as(this::asSslParameters).to(builder::sslParameters);
		map.from(settings::redirects).as(this::asHttpClientRedirect).to(builder::followRedirects);
		this.httpClientCustomizer.accept(builder);
		return builder.build();
	}

	private SSLParameters asSslParameters(SslBundle sslBundle) {
		SslOptions options = sslBundle.getOptions();
		SSLParameters parameters = new SSLParameters();
		parameters.setCipherSuites(options.getCiphers());
		parameters.setProtocols(options.getEnabledProtocols());
		return parameters;
	}

	private Redirect asHttpClientRedirect(Redirects redirects) {
		return switch (redirects) {
			case FOLLOW_WHEN_POSSIBLE, FOLLOW -> Redirect.NORMAL;
			case DONT_FOLLOW -> Redirect.NEVER;
		};
	}

	static class Classes {

		static final String HTTP_CLIENT = "java.net.http.HttpClient";

		static final boolean PRESENT = ClassUtils.isPresent(HTTP_CLIENT, null);

	}

}
