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

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.util.function.Consumer;

import javax.net.ssl.SSLParameters;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.util.Assert;

/**
 * Builder that can be used to create a JDK {@link HttpClient}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.5.0
 */
public final class JdkHttpClientBuilder {

	private final Consumer<HttpClient.Builder> customizer;

	public JdkHttpClientBuilder() {
		this(Empty.consumer());
	}

	private JdkHttpClientBuilder(Consumer<HttpClient.Builder> customizer) {
		this.customizer = customizer;
	}

	/**
	 * Return a new {@link JdkHttpClientBuilder} that applies additional customization to
	 * the underlying {@link java.net.http.HttpClient.Builder}.
	 * @param customizer the customizer to apply
	 * @return a new {@link JdkHttpClientBuilder} instance
	 */
	public JdkHttpClientBuilder withCustomizer(Consumer<HttpClient.Builder> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return new JdkHttpClientBuilder(this.customizer.andThen(customizer));
	}

	/**
	 * Build a new {@link HttpClient} instance with the given settings applied.
	 * @param settings the settings to apply
	 * @return a new {@link HttpClient} instance
	 */
	public HttpClient build(HttpClientSettings settings) {
		settings = (settings != null) ? settings : HttpClientSettings.DEFAULTS;
		Assert.isTrue(settings.readTimeout() == null, "'settings' must not have a 'readTimeout'");
		HttpClient.Builder builder = HttpClient.newBuilder();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::redirects).as(this::asHttpClientRedirect).to(builder::followRedirects);
		map.from(settings::connectTimeout).to(builder::connectTimeout);
		map.from(settings::sslBundle).as(SslBundle::createSslContext).to(builder::sslContext);
		map.from(settings::sslBundle).as(this::asSslParameters).to(builder::sslParameters);
		this.customizer.accept(builder);
		return builder.build();
	}

	private SSLParameters asSslParameters(SslBundle sslBundle) {
		SslOptions options = sslBundle.getOptions();
		SSLParameters parameters = new SSLParameters();
		parameters.setCipherSuites(options.getCiphers());
		parameters.setProtocols(options.getEnabledProtocols());
		return parameters;
	}

	private Redirect asHttpClientRedirect(HttpRedirects redirects) {
		return switch (redirects) {
			case FOLLOW_WHEN_POSSIBLE, FOLLOW -> Redirect.NORMAL;
			case DONT_FOLLOW -> Redirect.NEVER;
		};
	}

}
