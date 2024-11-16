/*
 * Copyright 2012-2024 the original author or authors.
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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.client.HttpClientProperties.Factory;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link ClientHttpRequestFactoryBuilder} and {@link ClientHttpRequestFactorySettings}.
 *
 * @author Phillip Webb
 * @since 3.4.0
 */
@AutoConfiguration(after = SslAutoConfiguration.class)
@ConditionalOnClass(ClientHttpRequestFactory.class)
@Conditional(NotReactiveWebApplicationCondition.class)
@EnableConfigurationProperties(HttpClientProperties.class)
public class HttpClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder(HttpClientProperties httpClientProperties) {
		Factory factory = httpClientProperties.getFactory();
		return (factory != null) ? factory.builder() : ClientHttpRequestFactoryBuilder.detect();
	}

	@Bean
	@ConditionalOnMissingBean
	ClientHttpRequestFactorySettings clientHttpRequestFactorySettings(HttpClientProperties httpClientProperties,
			ObjectProvider<SslBundles> sslBundles) {
		SslBundle sslBundle = getSslBundle(httpClientProperties.getSsl(), sslBundles);
		return new ClientHttpRequestFactorySettings(httpClientProperties.getRedirects(),
				httpClientProperties.getConnectTimeout(), httpClientProperties.getReadTimeout(), sslBundle);
	}

	private SslBundle getSslBundle(HttpClientProperties.Ssl properties, ObjectProvider<SslBundles> sslBundles) {
		String name = properties.getBundle();
		return (StringUtils.hasLength(name)) ? sslBundles.getObject().getBundle(name) : null;
	}

}
