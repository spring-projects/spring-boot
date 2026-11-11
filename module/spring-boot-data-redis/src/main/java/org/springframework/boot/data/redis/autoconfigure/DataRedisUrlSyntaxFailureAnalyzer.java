/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.autoconfigure;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@link DataRedisUrlSyntaxException}.
 *
 * @author Scott Frederick
 */
class DataRedisUrlSyntaxFailureAnalyzer extends AbstractFailureAnalyzer<DataRedisUrlSyntaxException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, DataRedisUrlSyntaxException cause) {
		try {
			URI uri = new URI(cause.getUrl());
			if ("redis-sentinel".equals(uri.getScheme())) {
				return new FailureAnalysis(getUnsupportedSchemeDescription(cause.getUrl(), uri.getScheme()),
						"Use spring.data.redis.sentinel properties instead of spring.data.redis.url to configure Redis sentinel addresses.",
						cause);
			}
			if ("redis-socket".equals(uri.getScheme())) {
				return new FailureAnalysis(getUnsupportedSchemeDescription(cause.getUrl(), uri.getScheme()),
						"Configure the appropriate Spring Data Redis connection beans directly instead of setting the property 'spring.data.redis.url'.",
						cause);
			}
			if (!"redis".equals(uri.getScheme()) && !"rediss".equals(uri.getScheme())) {
				return new FailureAnalysis(getUnsupportedSchemeDescription(cause.getUrl(), uri.getScheme()),
						"Use the scheme 'redis://' for insecure or 'rediss://' for secure Redis standalone configuration.",
						cause);
			}
		}
		catch (URISyntaxException ex) {
			// fall through to default description and action
		}
		return new FailureAnalysis(getDefaultDescription(cause.getUrl()),
				"Review the value of the property 'spring.data.redis.url'.", cause);
	}

	private String getDefaultDescription(String url) {
		return "The URL '" + url + "' is not valid for configuring Spring Data Redis. ";
	}

	private String getUnsupportedSchemeDescription(String url, String scheme) {
		return getDefaultDescription(url) + "The scheme '" + scheme + "' is not supported.";
	}

}
