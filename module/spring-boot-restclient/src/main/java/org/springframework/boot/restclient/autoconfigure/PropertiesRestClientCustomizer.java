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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.autoconfigure.ApiversionProperties;
import org.springframework.boot.http.client.autoconfigure.PropertiesApiVersionInserter;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ApiVersionFormatter;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

/**
 * {@link RestClientCustomizer} to apply {@link AbstractRestClientProperties}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class PropertiesRestClientCustomizer implements RestClientCustomizer {

	private final @Nullable AbstractRestClientProperties[] orderedProperties;

	private final @Nullable ApiVersionInserter apiVersionInserter;

	public PropertiesRestClientCustomizer(@Nullable ApiVersionInserter apiVersionInserter,
			@Nullable ApiVersionFormatter apiVersionFormatter,
			@Nullable AbstractRestClientProperties... orderedProperties) {
		this.orderedProperties = orderedProperties;
		this.apiVersionInserter = PropertiesApiVersionInserter.get(apiVersionInserter, apiVersionFormatter,
				Arrays.stream(orderedProperties).map(this::getApiVersion));
	}

	private @Nullable ApiversionProperties getApiVersion(@Nullable AbstractRestClientProperties properties) {
		return (properties != null) ? properties.getApiversion() : null;
	}

	@Override
	public void customize(RestClient.Builder builder) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.apiVersionInserter).to(builder::apiVersionInserter);
		for (int i = this.orderedProperties.length - 1; i >= 0; i--) {
			AbstractRestClientProperties properties = this.orderedProperties[i];
			if (properties != null) {
				map.from(properties::getBaseUrl).whenHasText().to(builder::baseUrl);
				map.from(properties::getDefaultHeader).as(this::putAllHeaders).to(builder::defaultHeaders);
				setDefaultApiVersion(builder, map, properties);
			}
		}
	}

	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	private void setDefaultApiVersion(Builder builder, PropertyMapper map, AbstractRestClientProperties properties) {
		map.from(properties.getApiversion())
			.as(ApiversionProperties::getDefaultVersion)
			.whenNonNull()
			.to(builder::defaultApiVersion);
	}

	private Consumer<HttpHeaders> putAllHeaders(Map<String, List<String>> defaultHeaders) {
		return (httpHeaders) -> httpHeaders.putAll(defaultHeaders);
	}

}
