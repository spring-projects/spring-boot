/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.autoconfigure.service;

import java.util.Collections;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.http.client.autoconfigure.HttpClientProperties;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientProperties.HttpServiceClientPropertiesRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.env.Environment;

/**
 * Properties for HTTP Service clients.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0.0
 */
@ImportRuntimeHints(HttpServiceClientPropertiesRuntimeHints.class)
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

	static class HttpServiceClientPropertiesRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			BindableRuntimeHintsRegistrar.forTypes(HttpClientProperties.class).registerHints(hints, classLoader);
		}

	}

}
