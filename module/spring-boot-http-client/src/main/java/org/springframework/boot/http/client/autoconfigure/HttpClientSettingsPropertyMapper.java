/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.Assert;

/**
 * Utility that can be used to map {@link HttpClientSettingsProperties} to
 * {@link HttpClientSettings}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class HttpClientSettingsPropertyMapper {

	private final @Nullable SslBundles sslBundles;

	private final HttpClientSettings settings;

	public HttpClientSettingsPropertyMapper(@Nullable SslBundles sslBundles, @Nullable HttpClientSettings settings) {
		this.sslBundles = sslBundles;
		this.settings = (settings != null) ? settings : HttpClientSettings.defaults();
	}

	public HttpClientSettings map(@Nullable HttpClientSettingsProperties properties) {
		HttpClientSettings settings = HttpClientSettings.defaults();
		if (properties != null) {
			PropertyMapper map = PropertyMapper.get();
			settings = map.from(properties::getRedirects).to(settings, HttpClientSettings::withRedirects);
			settings = map.from(properties::getConnectTimeout).to(settings, HttpClientSettings::withConnectTimeout);
			settings = map.from(properties::getReadTimeout).to(settings, HttpClientSettings::withReadTimeout);
			settings = map.from(properties::getCookieHandling).to(settings, HttpClientSettings::withCookieHandling);
			settings = map.from(properties::getSsl)
				.as(HttpClientSettingsProperties.Ssl::getBundle)
				.as(this::getSslBundle)
				.to(settings, HttpClientSettings::withSslBundle);
		}
		return settings.orElse(this.settings);
	}

	private SslBundle getSslBundle(String name) {
		Assert.state(this.sslBundles != null, "No 'sslBundles' available");
		return this.sslBundles.getBundle(name);
	}

}
