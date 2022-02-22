/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.couchbase;

import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import com.couchbase.client.core.env.IoConfig;
import com.couchbase.client.core.env.SecurityConfig;
import com.couchbase.client.core.env.TimeoutConfig;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.codec.JacksonJsonSerializer;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.env.ClusterEnvironment.Builder;
import com.couchbase.client.java.json.JsonValueModule;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties.Timeouts;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.ResourceUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Yulin Qin
 * @since 1.4.0
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnClass(Cluster.class)
@ConditionalOnProperty("spring.couchbase.connection-string")
@EnableConfigurationProperties(CouchbaseProperties.class)
public class CouchbaseAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ClusterEnvironment couchbaseClusterEnvironment(CouchbaseProperties properties,
			ObjectProvider<ClusterEnvironmentBuilderCustomizer> customizers) {
		Builder builder = initializeEnvironmentBuilder(properties);
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	@Bean(destroyMethod = "disconnect")
	@ConditionalOnMissingBean
	public Cluster couchbaseCluster(CouchbaseProperties properties, ClusterEnvironment couchbaseClusterEnvironment) {
		ClusterOptions options = ClusterOptions.clusterOptions(properties.getUsername(), properties.getPassword())
				.environment(couchbaseClusterEnvironment);
		return Cluster.connect(properties.getConnectionString(), options);
	}

	private ClusterEnvironment.Builder initializeEnvironmentBuilder(CouchbaseProperties properties) {
		ClusterEnvironment.Builder builder = ClusterEnvironment.builder();
		Timeouts timeouts = properties.getEnv().getTimeouts();
		builder.timeoutConfig(TimeoutConfig.kvTimeout(timeouts.getKeyValue()).analyticsTimeout(timeouts.getAnalytics())
				.kvDurableTimeout(timeouts.getKeyValueDurable()).queryTimeout(timeouts.getQuery())
				.viewTimeout(timeouts.getView()).searchTimeout(timeouts.getSearch())
				.managementTimeout(timeouts.getManagement()).connectTimeout(timeouts.getConnect())
				.disconnectTimeout(timeouts.getDisconnect()));
		CouchbaseProperties.Io io = properties.getEnv().getIo();
		builder.ioConfig(IoConfig.maxHttpConnections(io.getMaxEndpoints()).numKvConnections(io.getMinEndpoints())
				.idleHttpConnectionTimeout(io.getIdleHttpConnectionTimeout()));
		if (properties.getEnv().getSsl().getEnabled()) {
			builder.securityConfig(SecurityConfig.enableTls(true)
					.trustManagerFactory(getTrustManagerFactory(properties.getEnv().getSsl())));
		}
		return builder;
	}

	private TrustManagerFactory getTrustManagerFactory(CouchbaseProperties.Ssl ssl) {
		String resource = ssl.getKeyStore();
		try {
			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			KeyStore keyStore = loadKeyStore(resource, ssl.getKeyStorePassword());
			trustManagerFactory.init(keyStore);
			return trustManagerFactory;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load Couchbase key store '" + resource + "'", ex);
		}
	}

	private KeyStore loadKeyStore(String resource, String keyStorePassword) throws Exception {
		KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
		URL url = ResourceUtils.getURL(resource);
		try (InputStream stream = url.openStream()) {
			store.load(stream, (keyStorePassword != null) ? keyStorePassword.toCharArray() : null);
		}
		return store;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ObjectMapper.class)
	static class JacksonConfiguration {

		@Bean
		@ConditionalOnSingleCandidate(ObjectMapper.class)
		ClusterEnvironmentBuilderCustomizer jacksonClusterEnvironmentBuilderCustomizer(ObjectMapper objectMapper) {
			return new JacksonClusterEnvironmentBuilderCustomizer(
					objectMapper.copy().registerModule(new JsonValueModule()));
		}

	}

	private static final class JacksonClusterEnvironmentBuilderCustomizer
			implements ClusterEnvironmentBuilderCustomizer, Ordered {

		private final ObjectMapper objectMapper;

		private JacksonClusterEnvironmentBuilderCustomizer(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		@Override
		public void customize(Builder builder) {
			builder.jsonSerializer(JacksonJsonSerializer.create(this.objectMapper));
		}

		@Override
		public int getOrder() {
			return 0;
		}

	}

}
