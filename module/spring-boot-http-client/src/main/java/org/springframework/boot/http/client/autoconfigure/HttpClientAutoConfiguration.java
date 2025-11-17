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

package org.springframework.boot.http.client.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.HttpClientSettings;
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
	HttpClientSettings httpClientSettings(ObjectProvider<SslBundles> sslBundles, HttpClientsProperties properties) {
		HttpClientSettingsPropertyMapper propertyMapper = new HttpClientSettingsPropertyMapper(
				sslBundles.getIfAvailable(), null);
		return propertyMapper.map(properties);
	}

}
