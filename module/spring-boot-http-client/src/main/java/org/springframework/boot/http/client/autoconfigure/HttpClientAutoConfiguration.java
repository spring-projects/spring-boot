/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.InetAddressFilter;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HttpClientSettings}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = SslAutoConfiguration.class)
@EnableConfigurationProperties(HttpClientsProperties.class)
public final class HttpClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	HttpClientSettings httpClientSettings(HttpClientsProperties properties,
			ObjectProvider<SslBundles> sslBundlesProvider,
			ObjectProvider<InetAddressFilter> inetAddressFilterProvider) {
		InetAddressFilter inetAddressFilter = inetAddressFilterProvider.getIfAvailable();
		HttpClientSettings settings = (inetAddressFilter != null)
				? HttpClientSettings.defaults().withInetAddressFilter(inetAddressFilter)
				: HttpClientSettings.defaults();
		HttpClientSettingsPropertyMapper propertyMapper = new HttpClientSettingsPropertyMapper(
				sslBundlesProvider.getIfAvailable(), settings);
		return propertyMapper.map(properties);
	}

}
