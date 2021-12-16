/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.redis;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@link RedisUrlSyntaxException}.
 *
 * @author Scott Frederick
 */
class RedisUrlSyntaxFailureAnalyzer extends AbstractFailureAnalyzer<RedisUrlSyntaxException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, RedisUrlSyntaxException cause) {
		try {
			URI uri = new URI(cause.getUrl());
			if ("redis-sentinel".equals(uri.getScheme())) {
				return new FailureAnalysis(getUnsupportedSchemeDescription(cause.getUrl(), uri.getScheme()),
						"Use spring.redis.sentinel properties instead of spring.redis.url to configure Redis sentinel addresses.",
						cause);
			}
			if ("redis-socket".equals(uri.getScheme())) {
				return new FailureAnalysis(getUnsupportedSchemeDescription(cause.getUrl(), uri.getScheme()),
						"Configure the appropriate Spring Data Redis connection beans directly instead of setting the property 'spring.redis.url'.",
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
				"Review the value of the property 'spring.redis.url'.", cause);
	}

	private String getDefaultDescription(String url) {
		return "The URL '" + url + "' is not valid for configuring Spring Data Redis. ";
	}

	private String getUnsupportedSchemeDescription(String url, String scheme) {
		return getDefaultDescription(url) + "The scheme '" + scheme + "' is not supported.";
	}

}
