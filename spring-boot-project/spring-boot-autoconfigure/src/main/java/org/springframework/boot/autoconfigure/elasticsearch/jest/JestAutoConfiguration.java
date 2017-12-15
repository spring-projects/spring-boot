/*
 * Copyright 2012-2017 the original author or authors.
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

import java.time.Duration;
import java.util.List;

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
import org.springframework.boot.autoconfigure.elasticsearch.jest.JestProperties.Proxy;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

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

	private final List<HttpClientConfigBuilderCustomizer> builderCustomizers;

	public JestAutoConfiguration(JestProperties properties, ObjectProvider<Gson> gson,
			ObjectProvider<List<HttpClientConfigBuilderCustomizer>> builderCustomizers) {
		this.properties = properties;
		this.gsonProvider = gson;
		this.builderCustomizers = builderCustomizers.getIfAvailable();
	}

	@Bean(destroyMethod = "shutdownClient")
	@ConditionalOnMissingBean
	public JestClient jestClient() {
		JestClientFactory factory = new JestClientFactory();
		factory.setHttpClientConfig(createHttpClientConfig());
		return factory.getObject();
	}

	protected HttpClientConfig createHttpClientConfig() {
		HttpClientConfig.Builder builder = new HttpClientConfig.Builder(
				this.properties.getUris());
		PropertyMapper map = PropertyMapper.get();
		map.from(this.properties::getUsername).whenHasText().to((username) -> builder
				.defaultCredentials(username, this.properties.getPassword()));
		Proxy proxy = this.properties.getProxy();
		map.from(proxy::getHost).whenHasText().to((host) -> {
			Assert.notNull(proxy.getPort(), "Proxy port must not be null");
			builder.proxy(new HttpHost(host, proxy.getPort()));
		});
		map.from(this.gsonProvider::getIfUnique).whenNonNull().to(builder::gson);
		map.from(this.properties::isMultiThreaded).to(builder::multiThreaded);
		map.from(this.properties::getConnectionTimeout).whenNonNull()
				.asInt(Duration::toMillis).to(builder::connTimeout);
		map.from(this.properties::getReadTimeout).whenNonNull().asInt(Duration::toMillis)
				.to(builder::readTimeout);
		customize(builder);
		return builder.build();
	}

	private void customize(HttpClientConfig.Builder builder) {
		if (this.builderCustomizers != null) {
			for (HttpClientConfigBuilderCustomizer customizer : this.builderCustomizers) {
				customizer.customize(builder);
			}
		}
	}

}
