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

package org.springframework.boot.http.client.autoconfigure.service;

import java.util.Collections;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.http.client.autoconfigure.HttpClientProperties;
import org.springframework.core.env.Environment;

/**
 * Properties for HTTP Service clients.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0.0
 */
public class HttpServiceClientProperties {

	private final Map<String, HttpClientProperties> properties;

	HttpServiceClientProperties(Map<String, HttpClientProperties> properties) {
		this.properties = properties;

	}

	/**
	 * Return the {@link HttpClientProperties} for the given named client.
	 * @param name the service client name
	 * @return the properties or {@code null}
	 */
	public @Nullable HttpClientProperties get(String name) {
		return this.properties.get(name);
	}

	static HttpServiceClientProperties bind(Environment environment) {
		return new HttpServiceClientProperties(Binder.get(environment)
			.bind("spring.http.serviceclient", Bindable.mapOf(String.class, HttpClientProperties.class))
			.orElse(Collections.emptyMap()));
	}

}
