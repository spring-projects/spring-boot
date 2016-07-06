/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.elasticsearch.jest;

import com.google.gson.Gson;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import org.apache.http.HttpHost;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-Configuration} for Jest.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@Configuration
@ConditionalOnClass(JestClient.class)
@EnableConfigurationProperties(JestProperties.class)
@AutoConfigureAfter(GsonAutoConfiguration.class)
public class JestAutoConfiguration {

	private final JestProperties properties;

	private final ObjectProvider<Gson> gsonProvider;

	public JestAutoConfiguration(JestProperties properties,
			ObjectProvider<Gson> gsonProvider) {
		this.properties = properties;
		this.gsonProvider = gsonProvider;
	}

	@Bean
	@ConditionalOnMissingBean
	public JestClient jestClient() {
		JestClientFactory factory = new JestClientFactory();
		factory.setHttpClientConfig(createHttpClientConfig());
		return factory.getObject();
	}

	protected HttpClientConfig createHttpClientConfig() {
		HttpClientConfig.Builder builder = new HttpClientConfig.Builder(
				this.properties.getUris());
		if (StringUtils.hasText(this.properties.getUsername())) {
			builder.defaultCredentials(this.properties.getUsername(),
					this.properties.getPassword());
		}
		String proxyHost = this.properties.getProxy().getHost();
		if (StringUtils.hasText(proxyHost)) {
			Integer proxyPort = this.properties.getProxy().getPort();
			Assert.notNull(proxyPort, "Proxy port must not be null");
			builder.proxy(new HttpHost(proxyHost, proxyPort));
		}
		Gson gson = this.gsonProvider.getIfUnique();
		if (gson != null) {
			builder.gson(gson);
		}
		return builder.connTimeout(this.properties.getConnectionTimeout())
				.readTimeout(this.properties.getReadTimeout()).build();
	}

}
