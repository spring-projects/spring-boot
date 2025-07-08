/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.restclient.autoconfigure;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

/**
 * Configure {@link Builder RestClient.Builder} with sensible defaults.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code RestClient.Builder} whose configuration is based upon that produced by
 * auto-configuration.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
public class RestClientBuilderConfigurer {

	private final ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder;

	private final ClientHttpRequestFactorySettings requestFactorySettings;

	private final List<RestClientCustomizer> customizers;

	public RestClientBuilderConfigurer() {
		this(ClientHttpRequestFactoryBuilder.detect(), ClientHttpRequestFactorySettings.defaults(),
				Collections.emptyList());
	}

	RestClientBuilderConfigurer(ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder,
			ClientHttpRequestFactorySettings requestFactorySettings, List<RestClientCustomizer> customizers) {
		this.requestFactoryBuilder = requestFactoryBuilder;
		this.requestFactorySettings = requestFactorySettings;
		this.customizers = customizers;
	}

	/**
	 * Configure the specified {@link Builder RestClient.Builder}. The builder can be
	 * further tuned and default settings can be overridden.
	 * @param builder the {@link Builder RestClient.Builder} instance to configure
	 * @return the configured builder
	 */
	public RestClient.Builder configure(RestClient.Builder builder) {
		builder.requestFactory(this.requestFactoryBuilder.build(this.requestFactorySettings));
		applyCustomizers(builder);
		return builder;
	}

	private void applyCustomizers(Builder builder) {
		for (RestClientCustomizer customizer : this.customizers) {
			customizer.customize(builder);
		}
	}

}
