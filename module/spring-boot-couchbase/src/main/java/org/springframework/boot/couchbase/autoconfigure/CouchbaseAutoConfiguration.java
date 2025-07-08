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

package org.springframework.boot.couchbase.autoconfigure;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.TrustManagerFactory;

import com.couchbase.client.core.env.Authenticator;
import com.couchbase.client.core.env.CertificateAuthenticator;
import com.couchbase.client.core.env.PasswordAuthenticator;
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
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseAutoConfiguration.CouchbaseCondition;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseProperties.Authentication.Jks;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseProperties.Authentication.Pem;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseProperties.Ssl;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseProperties.Timeouts;
import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.pem.PemSslStore;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Yulin Qin
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 4.0.0
 */
@AutoConfiguration(afterName = "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
@ConditionalOnClass(Cluster.class)
@Conditional(CouchbaseCondition.class)
@EnableConfigurationProperties(CouchbaseProperties.class)
public class CouchbaseAutoConfiguration {

	private final ResourceLoader resourceLoader;

	private final CouchbaseProperties properties;

	CouchbaseAutoConfiguration(ResourceLoader resourceLoader, CouchbaseProperties properties) {
		this.resourceLoader = ApplicationResourceLoader.get(resourceLoader);
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(CouchbaseConnectionDetails.class)
	PropertiesCouchbaseConnectionDetails couchbaseConnectionDetails(ObjectProvider<SslBundles> sslBundles) {
		return new PropertiesCouchbaseConnectionDetails(this.properties, sslBundles.getIfAvailable());
	}

	@Bean
	@ConditionalOnMissingBean
	public ClusterEnvironment couchbaseClusterEnvironment(
			ObjectProvider<ClusterEnvironmentBuilderCustomizer> customizers,
			CouchbaseConnectionDetails connectionDetails) {
		Builder builder = initializeEnvironmentBuilder(connectionDetails);
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public Authenticator couchbaseAuthenticator(CouchbaseConnectionDetails connectionDetails) throws IOException {
		if (connectionDetails.getUsername() != null && connectionDetails.getPassword() != null) {
			return PasswordAuthenticator.create(connectionDetails.getUsername(), connectionDetails.getPassword());
		}
		Pem pem = this.properties.getAuthentication().getPem();
		if (pem.getCertificates() != null) {
			PemSslStoreDetails details = new PemSslStoreDetails(null, pem.getCertificates(), pem.getPrivateKey());
			PemSslStore store = PemSslStore.load(details);
			return CertificateAuthenticator.fromKey(store.privateKey(), pem.getPrivateKeyPassword(),
					store.certificates());
		}
		Jks jks = this.properties.getAuthentication().getJks();
		if (jks.getLocation() != null) {
			Resource resource = this.resourceLoader.getResource(jks.getLocation());
			String keystorePassword = jks.getPassword();
			try (InputStream inputStream = resource.getInputStream()) {
				KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
				store.load(inputStream, (keystorePassword != null) ? keystorePassword.toCharArray() : null);
				return CertificateAuthenticator.fromKeyStore(store, keystorePassword);
			}
			catch (GeneralSecurityException ex) {
				throw new IllegalStateException("Error reading Couchbase certificate store", ex);
			}
		}
		throw new IllegalStateException("Couchbase authentication requires username and password, or certificates");
	}

	@Bean(destroyMethod = "disconnect")
	@ConditionalOnMissingBean
	public Cluster couchbaseCluster(ClusterEnvironment couchbaseClusterEnvironment, Authenticator authenticator,
			CouchbaseConnectionDetails connectionDetails) {
		ClusterOptions options = ClusterOptions.clusterOptions(authenticator).environment(couchbaseClusterEnvironment);
		return Cluster.connect(connectionDetails.getConnectionString(), options);
	}

	private ClusterEnvironment.Builder initializeEnvironmentBuilder(CouchbaseConnectionDetails connectionDetails) {
		ClusterEnvironment.Builder builder = ClusterEnvironment.builder();
		Timeouts timeouts = this.properties.getEnv().getTimeouts();
		builder.timeoutConfig((config) -> config.kvTimeout(timeouts.getKeyValue())
			.analyticsTimeout(timeouts.getAnalytics())
			.kvDurableTimeout(timeouts.getKeyValueDurable())
			.queryTimeout(timeouts.getQuery())
			.viewTimeout(timeouts.getView())
			.searchTimeout(timeouts.getSearch())
			.managementTimeout(timeouts.getManagement())
			.connectTimeout(timeouts.getConnect())
			.disconnectTimeout(timeouts.getDisconnect()));
		CouchbaseProperties.Io io = this.properties.getEnv().getIo();
		builder.ioConfig((config) -> config.maxHttpConnections(io.getMaxEndpoints())
			.numKvConnections(io.getMinEndpoints())
			.idleHttpConnectionTimeout(io.getIdleHttpConnectionTimeout()));
		SslBundle sslBundle = connectionDetails.getSslBundle();
		if (sslBundle != null) {
			configureSsl(builder, sslBundle);
		}
		return builder;
	}

	private void configureSsl(Builder builder, SslBundle sslBundle) {
		Assert.state(!sslBundle.getOptions().isSpecified(), "SSL Options cannot be specified with Couchbase");
		builder.securityConfig((config) -> {
			config.enableTls(true);
			TrustManagerFactory trustManagerFactory = sslBundle.getManagers().getTrustManagerFactory();
			if (trustManagerFactory != null) {
				config.trustManagerFactory(trustManagerFactory);
			}
		});
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

	/**
	 * Condition that matches when {@code spring.couchbase.connection-string} has been
	 * configured or there is a {@link CouchbaseConnectionDetails} bean.
	 */
	static final class CouchbaseCondition extends AnyNestedCondition {

		CouchbaseCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty("spring.couchbase.connection-string")
		private static final class CouchbaseUrlCondition {

		}

		@ConditionalOnBean(CouchbaseConnectionDetails.class)
		private static final class CouchbaseConnectionDetailsCondition {

		}

	}

	/**
	 * Adapts {@link CouchbaseProperties} to {@link CouchbaseConnectionDetails}.
	 */
	static final class PropertiesCouchbaseConnectionDetails implements CouchbaseConnectionDetails {

		private final CouchbaseProperties properties;

		private final SslBundles sslBundles;

		PropertiesCouchbaseConnectionDetails(CouchbaseProperties properties, SslBundles sslBundles) {
			this.properties = properties;
			this.sslBundles = sslBundles;
		}

		@Override
		public String getConnectionString() {
			return this.properties.getConnectionString();
		}

		@Override
		public String getUsername() {
			return this.properties.getUsername();
		}

		@Override
		public String getPassword() {
			return this.properties.getPassword();
		}

		@Override
		public SslBundle getSslBundle() {
			Ssl ssl = this.properties.getEnv().getSsl();
			if (!ssl.getEnabled()) {
				return null;
			}
			if (StringUtils.hasLength(ssl.getBundle())) {
				Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
				return this.sslBundles.getBundle(ssl.getBundle());
			}
			return SslBundle.systemDefault();
		}

	}

}
