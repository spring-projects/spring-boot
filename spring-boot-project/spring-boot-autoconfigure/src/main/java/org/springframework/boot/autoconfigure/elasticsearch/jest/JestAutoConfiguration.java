/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.elasticsearch.jest;

import java.time.Duration;

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
 * {@link EnableAutoConfiguration Auto-configuration} for Jest.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 * @deprecated since 2.2.0 in favor of other auto-configured Elasticsearch clients
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JestClient.class)
@EnableConfigurationProperties(JestProperties.class)
@AutoConfigureAfter(GsonAutoConfiguration.class)
@Deprecated
public class JestAutoConfiguration {

	@Bean(destroyMethod = "shutdownClient")
	@ConditionalOnMissingBean
	public JestClient jestClient(JestProperties properties, ObjectProvider<Gson> gson,
			ObjectProvider<HttpClientConfigBuilderCustomizer> builderCustomizers) {
		JestClientFactory factory = new JestClientFactory();
		factory.setHttpClientConfig(createHttpClientConfig(properties, gson, builderCustomizers));
		return factory.getObject();
	}

	protected HttpClientConfig createHttpClientConfig(JestProperties properties, ObjectProvider<Gson> gson,
			ObjectProvider<HttpClientConfigBuilderCustomizer> builderCustomizers) {
		HttpClientConfig.Builder builder = new HttpClientConfig.Builder(properties.getUris());
		PropertyMapper map = PropertyMapper.get();
		map.from(properties::getUsername).whenHasText()
				.to((username) -> builder.defaultCredentials(username, properties.getPassword()));
		Proxy proxy = properties.getProxy();
		map.from(proxy::getHost).whenHasText().to((host) -> {
			Assert.notNull(proxy.getPort(), "Proxy port must not be null");
			builder.proxy(new HttpHost(host, proxy.getPort()));
		});
		map.from(gson::getIfUnique).whenNonNull().to(builder::gson);
		map.from(properties::isMultiThreaded).to(builder::multiThreaded);
		map.from(properties::getConnectionTimeout).whenNonNull().asInt(Duration::toMillis).to(builder::connTimeout);
		map.from(properties::getReadTimeout).whenNonNull().asInt(Duration::toMillis).to(builder::readTimeout);
		builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

}
