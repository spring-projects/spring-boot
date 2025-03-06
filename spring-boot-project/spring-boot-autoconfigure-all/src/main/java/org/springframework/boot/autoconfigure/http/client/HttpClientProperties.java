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

package org.springframework.boot.autoconfigure.http.client;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.HttpRedirects;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for a Spring's blocking HTTP
 * clients.
 *
 * @author Phillip Webb
 * @since 3.4.0
 * @see ClientHttpRequestFactorySettings
 * @deprecated since 3.5.0 for removal in 4.0.0 in favor of
 * {@link HttpClientSettingsProperties}
 */
@ConfigurationProperties("spring.http.client")
@Deprecated(since = "3.5.0", forRemoval = true)
public class HttpClientProperties extends AbstractHttpRequestFactoryProperties {

	@Override
	@DeprecatedConfigurationProperty(since = "3.5.0", replacement = "spring.http.client.settings.factory")
	public Factory getFactory() {
		return super.getFactory();
	}

	@Override
	@DeprecatedConfigurationProperty(since = "3.5.0", replacement = "spring.http.client.settings.redirects")
	public HttpRedirects getRedirects() {
		return super.getRedirects();
	}

	@Override
	@DeprecatedConfigurationProperty(since = "3.5.0", replacement = "spring.http.client.settings.connect-timeout")
	public Duration getConnectTimeout() {
		return super.getConnectTimeout();
	}

	@Override
	@DeprecatedConfigurationProperty(since = "3.5.0", replacement = "spring.http.client.settings.read-timeout")
	public Duration getReadTimeout() {
		return super.getReadTimeout();
	}

	@Override
	@DeprecatedConfigurationProperty(since = "3.5.0", replacement = "spring.http.client.settings.ssl")
	public Ssl getSsl() {
		return super.getSsl();
	}

}
